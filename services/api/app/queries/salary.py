from __future__ import annotations

from app.queries.common import HAS_SALARY, MEDIAN_SALARY, pct_expr, where_with


def grade_band(table: str, where: str) -> str:
    w = where_with(where, HAS_SALARY)
    return f"""
        SELECT
            arrayJoin(grades)            AS name,
            round(quantile(0.1)(salary)) AS min,
            {MEDIAN_SALARY}              AS median,
            round(quantile(0.9)(salary)) AS max,
            count()                      AS count
        FROM {table}
        {w}
        GROUP BY name
        ORDER BY median
    """


def by_direction(table: str, where: str, limit: int = 10) -> str:
    return f"""
        SELECT arrayJoin(fields) AS name, {MEDIAN_SALARY} AS median, count() AS count
        FROM {table}
        {where}
        GROUP BY name
        HAVING count >= 30
        ORDER BY median DESC
        LIMIT {limit}
    """


def by_experience(table: str, where: str) -> str:
    return f"""
        SELECT experience AS name, count() AS count, {MEDIAN_SALARY} AS median
        FROM {table}
        {where}
        GROUP BY name
        ORDER BY count DESC
    """


def transparency(table: str, where: str) -> str:
    return f"""
        SELECT {pct_expr(HAS_SALARY)} AS with_salary_pct,
               count() AS total
        FROM {table}
        {where}
    """


def by_currency(table: str, where: str, limit: int = 6) -> str:
    w = where_with(where, "currency != ''")
    return f"""
        SELECT currency AS name, count() AS count, {MEDIAN_SALARY} AS median
        FROM {table}
        {w}
        GROUP BY name
        ORDER BY count DESC
        LIMIT {limit}
    """
