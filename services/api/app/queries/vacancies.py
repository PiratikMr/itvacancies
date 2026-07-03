from __future__ import annotations

from app.queries.common import ACTIVE, order_by, where_with

_SORT_COLS = {
    "salary":     "salary",
    "grade":      "arrayElement(grades_sort, 1)",
    "experience": "experience_min_years * 1000 + experience_max_years",
    "date":       "published_at",
}

_EMPTIES = {
    "salary":     "salary <= 0",
    "grade":      "empty(grades_sort) or arrayElement(grades_sort, 1) = 255",
    "experience": "experience_min_years = 255",
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
        ORDER BY {order_by(sort, direction, _SORT_COLS, "date", "vacancy_id DESC", _EMPTIES)}
        LIMIT {limit} OFFSET {offset}
    """


def total(table: str, where: str) -> str:
    w = where_with(where, ACTIVE)
    return f"SELECT count() AS total FROM {table} {w}"
