from __future__ import annotations

from app.queries.common import ACTIVE, MEDIAN_SALARY, REMOTE, avg_close_days_expr, pct_expr


def kpis(table: str, where: str) -> str:
    return f"""
        SELECT
            count()                                   AS total,
            countIf({ACTIVE})                         AS active,
            {MEDIAN_SALARY}                           AS median_salary,
            {pct_expr(REMOTE)}                        AS remote_pct
        FROM {table}
        {where}
    """


def kpi_sparks(table: str, where: str) -> str:
    return f"""
        SELECT
            toStartOfWeek(published_at, 1)            AS w,
            count()                                   AS total,
            countIf({ACTIVE})                         AS active,
            {MEDIAN_SALARY}                           AS median_salary,
            {pct_expr(REMOTE)}                        AS remote_pct
        FROM {table}
        {where}
        GROUP BY w
        ORDER BY w
    """


def timeseries(table: str, where: str, bucket: str = "week") -> str:
    if bucket == "month":
        expr, step = "toStartOfMonth(published_at)", "toIntervalMonth(1)"
    else:
        expr, step = "toStartOfWeek(published_at, 1)", "toIntervalWeek(1)"
    return f"""
        SELECT {expr} AS period, count() AS count
        FROM {table}
        {where}
        GROUP BY period
        ORDER BY period WITH FILL STEP {step}
    """


def grades(table: str, where: str) -> str:
    return f"""
        SELECT arrayJoin(grades) AS name, count() AS count
        FROM {table}
        {where}
        GROUP BY name
        ORDER BY count DESC
    """


def directions(table: str, where: str, limit: int = 8) -> str:
    return f"""
        SELECT
            arrayJoin(fields) AS name,
            count()           AS count,
            {MEDIAN_SALARY}   AS median_salary
        FROM {table}
        {where}
        GROUP BY name
        ORDER BY count DESC
        LIMIT {limit}
    """


def top_skills(table: str, where: str, limit: int = 8) -> str:
    return f"""
        SELECT
            arrayJoin(skills) AS name,
            count()           AS count,
            {MEDIAN_SALARY}   AS median_salary
        FROM {table}
        {where}
        GROUP BY name
        ORDER BY count DESC
        LIMIT {limit}
    """


def formats(table: str, where: str) -> str:
    return f"""
        SELECT
            arrayJoin(schedules) AS name,
            count()              AS count,
            {MEDIAN_SALARY}      AS median_salary
        FROM {table}
        {where}
        GROUP BY name
        ORDER BY count DESC
    """


def platforms(table: str, where: str) -> str:
    return f"""
        SELECT
            platform               AS name,
            count()                AS count,
            {avg_close_days_expr()} AS avg_close_days
        FROM {table}
        {where}
        GROUP BY name
        ORDER BY count DESC
    """
