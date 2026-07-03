from __future__ import annotations

from app.queries.common import ACTIVE, CLOSED, MEDIAN_SALARY, order_by, where_with

_NON_EMPTY = "employer != ''"


def hiring_dynamics(table: str, where: str, bucket: str = "week") -> str:
    if bucket == "month":
        expr, step = "toStartOfMonth(published_at)", "toIntervalMonth(1)"
    else:
        expr, step = "toStartOfWeek(published_at, 1)", "toIntervalWeek(1)"
    w = where_with(where, _NON_EMPTY)
    return f"""
        SELECT
            {expr}                                    AS period,
            uniqExact(employer)                       AS employers,
            round(count() / uniqExact(employer), 1)   AS per_employer
        FROM {table}
        {w}
        GROUP BY period
        ORDER BY period WITH FILL STEP {step}
    """


def top_active(table: str, where: str, limit: int = 8) -> str:
    w = where_with(where, _NON_EMPTY, ACTIVE)
    return f"""
        SELECT employer AS name, count() AS active
        FROM {table}
        {w}
        GROUP BY name
        ORDER BY active DESC
        LIMIT {limit}
    """


def kpis(table: str, where: str) -> str:
    w = where_with(where, _NON_EMPTY)
    closed = f"{CLOSED} AND closed_at >= published_at"
    return f"""
        SELECT
            uniqExact(employer)                                AS unique_employers,
            uniqExactIf(employer, {ACTIVE})                    AS active_employers,
            round(count() / uniqExact(employer), 1)            AS avg_per_company,
            round(avgIf(dateDiff('day', published_at, closed_at), {closed})) AS avg_close_days
        FROM {table}
        {w}
    """


_TABLE_SORT = {"name": "name", "count": "count", "active": "active", "median": "median"}
_EMPTIES = {"median": "median <= 0"}


def top_employers(table: str, where: str, limit: int, offset: int,
                  sort: str = "count", direction: str = "desc") -> str:
    w = where_with(where, _NON_EMPTY)
    return f"""
        SELECT
            employer          AS name,
            count()           AS count,
            countIf({ACTIVE}) AS active,
            {MEDIAN_SALARY}   AS median
        FROM {table}
        {w}
        GROUP BY name
        ORDER BY {order_by(sort, direction, _TABLE_SORT, "count", "name ASC", _EMPTIES)}
        LIMIT {limit} OFFSET {offset}
    """
