from sqlalchemy import create_engine, text

import config

engine = create_engine(config.DB_URL, pool_pre_ping=True, pool_size=5)


def get_counts(dimension_name: str) -> tuple[int, int]:
    table = f"dim_{dimension_name}"
    with engine.connect() as conn:
        row = conn.execute(text(f"""
            SELECT
                COUNT(*) FILTER (WHERE is_reference = true)  AS ref_count,
                COUNT(*) FILTER (WHERE is_reference = false) AS cand_count
            FROM {table}
        """)).fetchone()
        return int(row[0]), int(row[1])


def get_records(dimension_name: str, is_reference: bool) -> list[tuple[int, str]]:
    table = f"dim_{dimension_name}"
    id_col = f"{dimension_name}_id"
    name_col = dimension_name
    with engine.connect() as conn:
        rows = conn.execute(text(f"""
            SELECT {id_col}, {name_col}
            FROM {table}
            WHERE is_reference = :is_ref
        """), {"is_ref": is_reference}).fetchall()
        return [(r[0], r[1]) for r in rows]


def _usage_subquery(dimension_name: str, relation_type: str) -> str:
    id_col = f"{dimension_name}_id"
    if relation_type == "bridge":
        bridge = f"bridge_vacancy_{dimension_name}"
        return f"SELECT {id_col} AS id, COUNT(*) AS cnt FROM {bridge} GROUP BY {id_col}"
    if relation_type == "fact":
        return (
            f"SELECT {id_col} AS id, COUNT(*) AS cnt FROM fact_vacancy "
            f"WHERE {id_col} IS NOT NULL GROUP BY {id_col}"
        )
    if relation_type == "language_bridge":
        return "SELECT language_id AS id, COUNT(*) AS cnt FROM bridge_vacancy_language GROUP BY language_id"
    if relation_type == "language_level_bridge":
        return "SELECT language_level_id AS id, COUNT(*) AS cnt FROM bridge_vacancy_language GROUP BY language_level_id"
    if relation_type == "country_dim":
        return (
            "SELECT l.country_id AS id, COUNT(*) AS cnt "
            "FROM bridge_vacancy_location b "
            "JOIN dim_location l ON b.location_id = l.location_id "
            "GROUP BY l.country_id"
        )
    raise ValueError(f"Неизвестный relation_type: {relation_type}")


_SORT_COLUMNS = {"mentions": "mentions", "name": "name", "id": "id"}


def list_records(
    dimension_name: str,
    relation_type: str,
    *,
    search: str = "",
    ref_filter: str = "all",
    min_cnt: int = 0,
    max_cnt: int | None = None,
    sort: str = "mentions",
    direction: str = "desc",
    limit: int = 100,
    offset: int = 0,
) -> tuple[list[dict], int]:
    table = f"dim_{dimension_name}"
    id_col = f"{dimension_name}_id"
    name_col = dimension_name
    usage = _usage_subquery(dimension_name, relation_type)

    sort_col = _SORT_COLUMNS.get(sort, "mentions")
    dir_sql = "ASC" if str(direction).lower() == "asc" else "DESC"

    ref = {"ref": True, "cand": False}.get(ref_filter, None)

    where = ["(:search = '' OR d.{nc} ILIKE '%' || :search || '%')".format(nc=name_col)]
    params: dict = {"search": search or ""}

    if ref is not None:
        where.append("d.is_reference = :ref")
        params["ref"] = ref

    where.append("COALESCE(u.cnt, 0) >= :min_cnt")
    params["min_cnt"] = int(min_cnt)
    if max_cnt is not None:
        where.append("COALESCE(u.cnt, 0) <= :max_cnt")
        params["max_cnt"] = int(max_cnt)

    where_sql = " AND ".join(where)

    base_from = f"""
        FROM {table} d
        LEFT JOIN ({usage}) u ON u.id = d.{id_col}
        WHERE {where_sql}
    """

    with engine.connect() as conn:
        total = conn.execute(
            text(f"SELECT COUNT(*) {base_from}"), params
        ).scalar_one()

        rows = conn.execute(
            text(f"""
                SELECT d.{id_col} AS id, d.{name_col} AS name,
                       d.is_reference AS is_reference, COALESCE(u.cnt, 0) AS mentions
                {base_from}
                ORDER BY {sort_col} {dir_sql}, d.{id_col} ASC
                LIMIT :limit OFFSET :offset
            """),
            {**params, "limit": int(limit), "offset": int(offset)},
        ).mappings().all()

    return [dict(r) for r in rows], int(total)


def set_reference(dimension_name: str, ids: list[int], value: bool) -> int:
    table = f"dim_{dimension_name}"
    id_col = f"{dimension_name}_id"
    with engine.begin() as conn:
        result = conn.execute(
            text(f"UPDATE {table} SET is_reference = :val WHERE {id_col} = ANY(:ids)"),
            {"val": bool(value), "ids": list(ids)},
        )
        return result.rowcount


def rename_record(dimension_name: str, record_id: int, new_name: str) -> None:
    table = f"dim_{dimension_name}"
    id_col = f"{dimension_name}_id"
    name_col = dimension_name
    with engine.begin() as conn:
        conn.execute(
            text(f"UPDATE {table} SET {name_col} = :name WHERE {id_col} = :id"),
            {"name": new_name, "id": record_id},
        )


def delete_records(dimension_name: str, relation_type: str, ids: list[int]) -> int:
    id_col = f"{dimension_name}_id"
    dim_table = f"dim_{dimension_name}"
    mapping_table = f"mapping_dim_{dimension_name}"
    ids = list(ids)

    with engine.begin() as conn:
        conn.execute(
            text(f"DELETE FROM {mapping_table} WHERE {id_col} = ANY(:ids)"), {"ids": ids}
        )

        if relation_type == "bridge":
            bridge_table = f"bridge_vacancy_{dimension_name}"
            conn.execute(
                text(f"DELETE FROM {bridge_table} WHERE {id_col} = ANY(:ids)"), {"ids": ids}
            )
        elif relation_type == "fact":
            conn.execute(
                text(f"UPDATE fact_vacancy SET {id_col} = NULL WHERE {id_col} = ANY(:ids)"),
                {"ids": ids},
            )
        elif relation_type == "language_bridge":
            conn.execute(
                text("DELETE FROM bridge_vacancy_language WHERE language_id = ANY(:ids)"),
                {"ids": ids},
            )
        elif relation_type == "language_level_bridge":
            conn.execute(
                text("DELETE FROM bridge_vacancy_language WHERE language_level_id = ANY(:ids)"),
                {"ids": ids},
            )
        elif relation_type == "country_dim":
            conn.execute(
                text("UPDATE dim_location SET country_id = 0 WHERE country_id = ANY(:ids)"),
                {"ids": ids},
            )
        else:
            raise ValueError(f"Неизвестный relation_type: {relation_type}")

        result = conn.execute(
            text(f"DELETE FROM {dim_table} WHERE {id_col} = ANY(:ids)"), {"ids": ids}
        )
        return result.rowcount


def apply_normalization_batch(dimension_name: str, relation_type: str, merges: list[dict]):
    id_col = f"{dimension_name}_id"
    dim_table = f"dim_{dimension_name}"
    mapping_table = f"mapping_dim_{dimension_name}"

    with engine.begin() as conn:
        for m in merges:
            candidate_id = m["candidate_id"]
            golden_id = m["golden_id"]

            conn.execute(text(f"""
                INSERT INTO {mapping_table} ({id_col}, mapped_value, is_canonical)
                SELECT :g_id, mapped_value, false
                FROM {mapping_table}
                WHERE {id_col} = :c_id
                ON CONFLICT ({id_col}, mapped_value) DO NOTHING
            """), {"g_id": golden_id, "c_id": candidate_id})

            conn.execute(text(f"""
                DELETE FROM {mapping_table}
                WHERE {id_col} = :c_id
            """), {"c_id": candidate_id})

            if relation_type == "bridge":
                bridge_table = f"bridge_vacancy_{dimension_name}"
                conn.execute(text(f"""
                    INSERT INTO {bridge_table} (vacancy_id, {id_col})
                    SELECT vacancy_id, :g_id
                    FROM {bridge_table}
                    WHERE {id_col} = :c_id
                    ON CONFLICT ({id_col}, vacancy_id) DO NOTHING
                """), {"g_id": golden_id, "c_id": candidate_id})

                conn.execute(text(f"""
                    DELETE FROM {bridge_table}
                    WHERE {id_col} = :c_id
                """), {"c_id": candidate_id})

            elif relation_type == "fact":
                conn.execute(text(f"""
                    UPDATE fact_vacancy
                    SET {id_col} = :g_id
                    WHERE {id_col} = :c_id
                """), {"g_id": golden_id, "c_id": candidate_id})

            elif relation_type == "language_bridge":
                conn.execute(text("""
                    INSERT INTO bridge_vacancy_language (vacancy_id, language_id, language_level_id)
                    SELECT vacancy_id, :g_id, language_level_id
                    FROM bridge_vacancy_language
                    WHERE language_id = :c_id
                    ON CONFLICT (vacancy_id, language_id, language_level_id) DO NOTHING
                """), {"g_id": golden_id, "c_id": candidate_id})

                conn.execute(text("""
                    DELETE FROM bridge_vacancy_language WHERE language_id = :c_id
                """), {"c_id": candidate_id})

            elif relation_type == "language_level_bridge":
                conn.execute(text("""
                    INSERT INTO bridge_vacancy_language (vacancy_id, language_id, language_level_id)
                    SELECT vacancy_id, language_id, :g_id
                    FROM bridge_vacancy_language
                    WHERE language_level_id = :c_id
                    ON CONFLICT (vacancy_id, language_id, language_level_id) DO NOTHING
                """), {"g_id": golden_id, "c_id": candidate_id})

                conn.execute(text("""
                    DELETE FROM bridge_vacancy_language WHERE language_level_id = :c_id
                """), {"c_id": candidate_id})

            elif relation_type == "country_dim":
                conn.execute(text("""
                    UPDATE dim_location SET country_id = :g_id WHERE country_id = :c_id
                """), {"g_id": golden_id, "c_id": candidate_id})

            else:
                raise ValueError(f"Неизвестный relation_type: {relation_type}")

            conn.execute(text(f"""
                DELETE FROM {dim_table}
                WHERE {id_col} = :c_id
            """), {"c_id": candidate_id})
