from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends, Query

from app.database import execute_query
from app.filters import FilterBuilder, parse_filters
from app.queries import vacancies as q
from app.schema import VACANCIES

router = APIRouter(prefix="/api/v1", tags=["vacancies"])

_builder = FilterBuilder(VACANCIES)
_TABLE = VACANCIES.full_name


@router.get("/vacancies")
def get_vacancies(
    filters: Annotated[dict[str, Any], Depends(parse_filters)],
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
    offset: Annotated[int, Query(ge=0)] = 0,
    sort: Annotated[str, Query()] = "date",
    direction: Annotated[str, Query(alias="dir", pattern="^(asc|desc)$")] = "desc",
) -> dict[str, Any]:
    where = _builder.build(filters)
    total = (execute_query(q.total(_TABLE, where)) or [{}])[0].get("total", 0)
    return {
        "rows": execute_query(q.rows(_TABLE, where, limit, offset, sort, direction)),
        "total": total,
        "limit": limit,
        "offset": offset,
    }
