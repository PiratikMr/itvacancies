from __future__ import annotations

from app.queries.common import MEDIAN_SALARY, order_by

_ENGLISH = "arrayExists(x -> x.1 = 'Английский', languages)"
MIN_SKILL_COUNT = 20


def kpis(table: str, where: str) -> str:
    return f"""
        SELECT
            round(avg(length(skills)), 1)                      AS avg_skills,
            round(100 * countIf({_ENGLISH}) / count(), 1)      AS english_pct,
            count()                                            AS total
        FROM {table}
        {where}
    """


def market_median(table: str, where: str) -> str:
    return f"SELECT {MEDIAN_SALARY} AS median FROM {table} {where}"


def top_demand(table: str, where: str) -> str:
    return f"""
        SELECT arrayJoin(skills) AS name, count() AS count
        FROM {table} {where}
        GROUP BY name ORDER BY count DESC LIMIT 1
    """


def top_paid(table: str, where: str) -> str:
    return f"""
        SELECT arrayJoin(skills) AS name, {MEDIAN_SALARY} AS median, count() AS count
        FROM {table} {where}
        GROUP BY name HAVING count >= {MIN_SKILL_COUNT}
        ORDER BY median DESC LIMIT 1
    """


def english_levels(table: str, where: str) -> str:
    return f"""
        SELECT
            arrayJoin(arrayFilter(x -> x.1 = 'Английский', languages)).2 AS level,
            count()         AS count,
            {MEDIAN_SALARY} AS median
        FROM {table} {where}
        GROUP BY level
        HAVING level != ''
        ORDER BY count DESC
    """


def top_pairs(table: str, where: str, limit: int = 11) -> str:
    return f"""
        SELECT s1 AS skill_a, s2 AS skill_b, count() AS count, {MEDIAN_SALARY} AS median
        FROM (
            SELECT s1, s2, salary
            FROM {table}
            ARRAY JOIN skills AS s1
            ARRAY JOIN skills AS s2
            {where}
        )
        WHERE s1 < s2
        GROUP BY s1, s2
        ORDER BY count DESC
        LIMIT {limit}
    """


def by_field(table: str, where: str, n_fields: int = 10, n_skills: int = 5) -> str:
    return f"""
        SELECT field, skill, count() AS count, any(field_total) AS field_total
        FROM (
            SELECT arrayJoin(fields) AS field, arrayJoin(skills) AS skill
            FROM {table} {where}
        ) p
        INNER JOIN (
            SELECT arrayJoin(fields) AS field, count() AS field_total
            FROM {table} {where}
            GROUP BY field ORDER BY field_total DESC LIMIT {n_fields}
        ) t USING (field)
        GROUP BY field, skill
        ORDER BY field_total DESC, field, count DESC
        LIMIT {n_skills} BY field
    """


_TABLE_SORT = {"name": "name", "count": "count", "median": "median"}
_EMPTIES = {"median": "median <= 0"}


def skills_table(table: str, where: str, limit: int, offset: int,
                 sort: str = "count", direction: str = "desc") -> str:
    return f"""
        SELECT arrayJoin(skills) AS name, count() AS count, {MEDIAN_SALARY} AS median
        FROM {table} {where}
        GROUP BY name HAVING count >= {MIN_SKILL_COUNT}
        ORDER BY {order_by(sort, direction, _TABLE_SORT, "count", "name ASC", _EMPTIES)}
        LIMIT {limit} OFFSET {offset}
    """


def skills_total(table: str, where: str) -> str:
    return f"""
        SELECT count() AS total FROM (
            SELECT arrayJoin(skills) AS name, count() AS c
            FROM {table} {where}
            GROUP BY name HAVING c >= {MIN_SKILL_COUNT}
        )
    """
