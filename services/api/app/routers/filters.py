from __future__ import annotations

from typing import Any

from fastapi import APIRouter

from app.database import execute_query
from app.schema import VACANCIES, ColumnType

router = APIRouter(prefix="/api/v1", tags=["filters"])

_TABLE = VACANCIES.full_name
_MAX_OPTIONS = 200


def _option_sql(expr: str) -> str:
    return f"""
        SELECT {expr} AS val, count() AS c
        FROM {_TABLE}
        WHERE val != ''
        GROUP BY val
        ORDER BY c DESC
        LIMIT {_MAX_OPTIONS}
    """


@router.get("/filters")
def get_filters() -> dict[str, Any]:
    options: dict[str, list[str]] = {}

    for name, col in VACANCIES.get_filterable().items():
        if col.type == ColumnType.SCALAR:
            expr = col.name
        elif col.type == ColumnType.ARRAY:
            expr = f"arrayJoin({col.name})"
        elif col.type == ColumnType.TUPLE_ARRAY and col.parent_column and col.tuple_field:
            expr = f"arrayJoin({col.parent_column}).{col.tuple_field}"
        else:
            continue

        rows = execute_query(_option_sql(expr))
        options[name] = [r["val"] for r in rows]

    salary_rows = execute_query(
        f"SELECT round(min(salary)) AS min, round(max(salary)) AS max "
        f"FROM {_TABLE} WHERE salary > 0"
    )
    date_rows = execute_query(
        f"SELECT toString(min(published_at)) AS min, toString(max(published_at)) AS max "
        f"FROM {_TABLE}"
    )

    return {
        "options": options,
        "salary": salary_rows[0] if salary_rows else {},
        "published_at": date_rows[0] if date_rows else {},
    }
