from __future__ import annotations

from app.queries.common import ACTIVE, order_by, where_with

_SORT_COLS = {
    "salary":     "salary",
    "grade":      "arrayElement(grades_sort, 1)",
    "experience": "experience_min_years * 1000 + experience_max_years",
    "date":       "published_at",
}


def rows(table: str, where: str, limit: int, offset: int, sort: str, direction: str) -> str:
    w = where_with(where, ACTIVE)
    cols = _SORT_COLS
    if sort in ("experience", "grade"):
        unspecified_last = -1 if direction == "desc" else 999999
        if sort == "experience":
            cols = {**_SORT_COLS, "experience": f"""
                if(experience_min_years = 255, {unspecified_last},
                   toInt32(experience_min_years) * 1000 + toInt32(experience_max_years))
            """}
        else:
            cols = {**_SORT_COLS, "grade": f"""
                if(empty(grades_sort) or arrayElement(grades_sort, 1) = 255, {unspecified_last},
                   toInt32(arrayElement(grades_sort, 1)))
            """}
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
        ORDER BY {order_by(sort, direction, cols, "date", "vacancy_id DESC")}
        LIMIT {limit} OFFSET {offset}
    """


def total(table: str, where: str) -> str:
    w = where_with(where, ACTIVE)
    return f"SELECT count() AS total FROM {table} {w}"
