from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends

from app.database import execute_query
from app.filters import FilterBuilder, parse_filters
from app.queries import geo as q
from app.schema import VACANCIES

router = APIRouter(prefix="/api/v1", tags=["geo"])

_builder = FilterBuilder(VACANCIES)
_TABLE = VACANCIES.full_name


@router.get("/geo")
def get_geo(filters: Annotated[dict[str, Any], Depends(parse_filters)]) -> dict[str, Any]:
    where = _builder.build(filters)

    countries = execute_query(q.by_country(_TABLE, where))
    cities_total = (execute_query(q.cities_total(_TABLE, where)) or [{}])[0].get("cities", 0)

    total_loc = sum(c["count"] for c in countries) or 1
    russia = next((c["count"] for c in countries if c["name"] == "Россия"), 0)
    outside_pct = round(100 * (total_loc - russia) / total_loc, 1)
    median_outside = (execute_query(q.median_outside_ru(_TABLE, where)) or [{}])[0].get("median", 0)

    return {
        "kpis": {
            "countries": len(countries),
            "cities": cities_total,
            "outside_russia_pct": outside_pct,
            "median_outside": median_outside,
        },
        "map": execute_query(q.map_points(_TABLE, where)),
        "countries": countries,
    }
