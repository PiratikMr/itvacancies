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
    # One representative vacancy per exact coordinate (no rounding — points are
    # kept as-is). Among vacancies sharing a coordinate we prefer one with a
    # stated salary, then the most recent; title/salary/url all come from that
    # same row. Payload is intentionally minimal: only what the map shows.
    w = where_with(where, "latitude != 200 AND longitude != 200")
    rank = "(salary > 0, published_at)"
    return f"""
        SELECT lat, lng, rep.1 AS title, rep.2 AS salary, rep.3 AS url
        FROM (
            SELECT
                latitude                              AS lat,
                longitude                             AS lng,
                argMax((title, salary, url), {rank})  AS rep,
                count()                               AS cnt
            FROM {table}
            {w}
            GROUP BY lat, lng
        )
        ORDER BY cnt DESC
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
