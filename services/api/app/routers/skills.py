from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends, Query

from app.database import execute_query
from app.filters import FilterBuilder, parse_filters
from app.queries import skills as q
from app.schema import VACANCIES

router = APIRouter(prefix="/api/v1", tags=["skills"])

_builder = FilterBuilder(VACANCIES)
_TABLE = VACANCIES.full_name


@router.get("/skills")
def get_skills(
    filters: Annotated[dict[str, Any], Depends(parse_filters)],
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
    offset: Annotated[int, Query(ge=0)] = 0,
    sort: Annotated[str, Query()] = "count",
    direction: Annotated[str, Query(alias="dir", pattern="^(asc|desc)$")] = "desc",
) -> dict[str, Any]:
    where = _builder.build(filters)

    kpi = (execute_query(q.kpis(_TABLE, where)) or [{}])[0]
    demand = (execute_query(q.top_demand(_TABLE, where)) or [{}])[0]
    paid = (execute_query(q.top_paid(_TABLE, where)) or [{}])[0]
    market = (execute_query(q.market_median(_TABLE, where)) or [{}])[0].get("median")
    total = (execute_query(q.skills_total(_TABLE, where)) or [{}])[0].get("total", 0)

    english = execute_query(q.english_levels(_TABLE, where))
    with_english = sum(r["count"] for r in english)
    not_required = max((kpi.get("total") or 0) - with_english, 0)

    return {
        "kpis": {
            "avg_skills": kpi.get("avg_skills"),
            "english_pct": kpi.get("english_pct"),
            "top_demand": {"name": demand.get("name"), "count": demand.get("count")},
            "top_paid": {"name": paid.get("name"), "median": paid.get("median")},
        },
        "english": {"levels": english, "not_required": not_required, "total": kpi.get("total")},
        "market_median": market,
        "by_field": execute_query(q.by_field(_TABLE, where)),
        "pairs": execute_query(q.top_pairs(_TABLE, where)),
        "table": {
            "rows": execute_query(q.skills_table(_TABLE, where, limit, offset, sort, direction)),
            "total": total,
            "limit": limit,
            "offset": offset,
        },
    }
