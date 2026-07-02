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
