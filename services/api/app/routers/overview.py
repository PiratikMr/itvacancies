from __future__ import annotations

from datetime import date
from typing import Annotated, Any

from fastapi import APIRouter, Depends

from app.database import execute_query
from app.filters import FilterBuilder, parse_filters
from app.queries import overview as q
from app.schema import VACANCIES

router = APIRouter(prefix="/api/v1", tags=["overview"])

_builder = FilterBuilder(VACANCIES)
_TABLE = VACANCIES.full_name

_GRADE_OTHER_THRESHOLD = 5.0


def _grades_as_shares(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    total = sum(r["count"] for r in rows) or 1
    shares: list[dict[str, Any]] = []
    other_count = 0
    for r in rows:
        pct = r["count"] / total * 100
        if pct < _GRADE_OTHER_THRESHOLD:
            other_count += r["count"]
        else:
            shares.append({"name": r["name"], "pct": round(pct, 1)})
    if other_count:
        shares.append({"name": "Другое", "pct": round(other_count / total * 100, 1)})
    return shares


@router.get("/overview")
def get_overview(
    filters: Annotated[dict[str, Any], Depends(parse_filters)],
) -> dict[str, Any]:
    where = _builder.build(filters)

    span_from = filters.get("published_at_from")
    bucket = "month" if (span_from is None or (date.today() - span_from).days > 280) else "week"

    kpi = (execute_query(q.kpis(_TABLE, where)) or [{}])[0]

    spark_rows = execute_query(q.kpi_sparks(_TABLE, where))[-16:]
    # New dict (don't mutate the cached query result).
    kpi = {
        **kpi,
        "spark": {
            metric: [r[metric] for r in spark_rows]
            for metric in ("total", "active", "median_salary", "remote_pct")
        },
    }

    return {
        "kpis": kpi,
        "timeseries": execute_query(q.timeseries(_TABLE, where, bucket)),
        "grades": _grades_as_shares(execute_query(q.grades(_TABLE, where))),
        "directions": execute_query(q.directions(_TABLE, where)),
        "top_skills": execute_query(q.top_skills(_TABLE, where)),
        "formats": execute_query(q.formats(_TABLE, where)),
        "platforms": execute_query(q.platforms(_TABLE, where)),
    }
