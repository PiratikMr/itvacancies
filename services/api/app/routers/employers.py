from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends, Query

from app.database import execute_query
from app.filters import FilterBuilder, parse_filters
from app.queries import employers as q
from app.schema import VACANCIES

router = APIRouter(prefix="/api/v1", tags=["employers"])

_builder = FilterBuilder(VACANCIES)
_TABLE = VACANCIES.full_name


@router.get("/employers")
def get_employers(
    filters: Annotated[dict[str, Any], Depends(parse_filters)],
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
    offset: Annotated[int, Query(ge=0)] = 0,
    sort: Annotated[str, Query()] = "count",
    direction: Annotated[str, Query(alias="dir", pattern="^(asc|desc)$")] = "desc",
) -> dict[str, Any]:
    where = _builder.build(filters)

    kpi = (execute_query(q.kpis(_TABLE, where)) or [{}])[0]

    return {
        "kpis": kpi,
        "dynamics": execute_query(q.hiring_dynamics(_TABLE, where)),
        "top_active": execute_query(q.top_active(_TABLE, where)),
        "table": {
            "rows": execute_query(q.top_employers(_TABLE, where, limit, offset, sort, direction)),
            "total": kpi.get("unique_employers", 0),
            "limit": limit,
            "offset": offset,
        },
    }
