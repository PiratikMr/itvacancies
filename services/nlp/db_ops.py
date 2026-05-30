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
