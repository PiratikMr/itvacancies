from __future__ import annotations

from app.queries.common import ACTIVE, order_by, where_with

_SORT_COLS = {
    "title":      "title",
    "salary":     "salary",
    "grade":      "arrayElement(grades, 1)",
    "experience": "experience",
    "format":     "arrayElement(schedules, 1)",
    "platform":   "platform",
    "date":       "published_at",
}


def rows(table: str, where: str, limit: int, offset: int, sort: str, direction: str) -> str:
    w = where_with(where, ACTIVE)
    return f"""
        SELECT
            vacancy_id,
            title,
            employer,
            url,
            salary,
            grades,
            experience,
            schedules,
            platform,
            published_at
        FROM {table}
        {w}
        ORDER BY {order_by(sort, direction, _SORT_COLS, "date", "vacancy_id DESC")}
        LIMIT {limit} OFFSET {offset}
    """


def total(table: str, where: str) -> str:
    w = where_with(where, ACTIVE)
    return f"SELECT count() AS total FROM {table} {w}"
