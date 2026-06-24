from __future__ import annotations

from app.queries.common import MEDIAN_SALARY, REMOTE, where_with

_NOISE = "('', 'Неизвестно', 'Другие регионы')"


def by_country(table: str, where: str) -> str:
    return f"""
        SELECT
            loc.2                                       AS name,
            count()                                     AS count,
            uniqExact(loc.1)                            AS cities,
            {MEDIAN_SALARY}                             AS median,
            round(100 * countIf({REMOTE}) / count(), 1) AS remote_pct
        FROM (
            SELECT arrayJoin(locations) AS loc, salary, schedules
            FROM {table} {where}
        )
        WHERE loc.2 NOT IN {_NOISE}
        GROUP BY name
        ORDER BY count DESC
    """


def map_points(table: str, where: str) -> str:
    w = where_with(where, "latitude != 200 AND longitude != 200")
    return f"""
        SELECT
            latitude                          AS lat,
            longitude                         AS lng,
            any(title)                        AS title,
            any(arrayElement(locations.1, 1)) AS city,
            count()                           AS count,
            {MEDIAN_SALARY}                   AS median,
            round(100 * countIf({REMOTE}) / count(), 1) AS remote_pct
        FROM {table}
        {w}
        GROUP BY lat, lng
        ORDER BY count DESC
    """


def median_outside_ru(table: str, where: str) -> str:
    cond = ("salary > 0 AND arrayExists(x -> x.2 NOT IN "
            "('Россия', '', 'Неизвестно', 'Другие регионы'), locations)")
    return (f"SELECT round(if(isNaN(medianIf(salary, {cond})), 0, "
            f"medianIf(salary, {cond}))) AS median FROM {table} {where}")


def cities_total(table: str, where: str) -> str:
    return f"""
        SELECT uniqExact(loc.1) AS cities
        FROM (SELECT arrayJoin(locations) AS loc FROM {table} {where})
        WHERE loc.1 != ''
    """
