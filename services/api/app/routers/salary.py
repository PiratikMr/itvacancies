from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends

from app.database import execute_query
from app.filters import FilterBuilder, parse_filters
from app.queries import salary as q
from app.schema import VACANCIES

router = APIRouter(prefix="/api/v1", tags=["salary"])

_builder = FilterBuilder(VACANCIES)
_TABLE = VACANCIES.full_name


def _median_for(bands: list[dict[str, Any]], grade: str) -> float | None:
    for b in bands:
        if b["name"] == grade:
            return b["median"]
    return None


@router.get("/salary")
def get_salary(filters: Annotated[dict[str, Any], Depends(parse_filters)]) -> dict[str, Any]:
    where = _builder.build(filters)

    bands = execute_query(q.grade_band(_TABLE, where))

    senior = _median_for(bands, "Сеньор")
    middle = _median_for(bands, "Миддл")
    junior = _median_for(bands, "Джуниор")
    ratio = round(senior / junior, 1) if senior and junior else None

    return {
        "kpis": {
            "senior_median": senior,
            "middle_median": middle,
            "junior_median": junior,
            "sr_jr_ratio": ratio,
        },
        "grade_band": bands,
        "by_direction": execute_query(q.by_direction(_TABLE, where)),
        "by_experience": execute_query(q.by_experience(_TABLE, where)),
        "transparency": (execute_query(q.transparency(_TABLE, where)) or [{}])[0],
        "by_currency": execute_query(q.by_currency(_TABLE, where)),
    }
