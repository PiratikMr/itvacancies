from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends, HTTPException, Path

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
        "countries": countries,
    }


@router.get("/geo/points")
def get_geo_points(filters: Annotated[dict[str, Any], Depends(parse_filters)]) -> dict[str, Any]:
    rows = execute_query(q.map_points(_TABLE, where=_builder.build(filters)))
    return {
        "lat": [r["lat"] for r in rows],
        "lng": [r["lng"] for r in rows],
        "ids": [r["id"] for r in rows],
    }


@router.get("/geo/point/{vacancy_id}")
def get_geo_point(vacancy_id: Annotated[int, Path(ge=0)]) -> dict[str, Any]:
    rows = execute_query(q.point_detail(_TABLE), {"id": vacancy_id})
    if not rows:
        raise HTTPException(status_code=404, detail="Вакансия не найдена")
    return rows[0]
