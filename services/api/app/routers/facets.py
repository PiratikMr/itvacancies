from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends, Query

from app.database import execute_query
from app.filters import FilterBuilder, parse_filters
from app.queries import facets as q
from app.queries.facets import FACET_EXPR
from app.schema import VACANCIES

router = APIRouter(prefix="/api/v1", tags=["facets"])

_builder = FilterBuilder(VACANCIES)
_TABLE = VACANCIES.full_name


def _where_excluding(filters: dict[str, Any], facet: str) -> str:
    return _builder.build({k: v for k, v in filters.items() if k != facet})


@router.get("/facets")
def get_facets(filters: Annotated[dict[str, Any], Depends(parse_filters)]) -> dict[str, Any]:
    facets: dict[str, list[str]] = {}
    for facet, expr in FACET_EXPR.items():
        where = _where_excluding(filters, facet)
        rows = execute_query(q.options(_TABLE, where, expr))
        facets[facet] = [r["v"] for r in rows]

    salary = (execute_query(
        f"SELECT round(min(salary)) AS min, round(max(salary)) AS max "
        f"FROM {_TABLE} WHERE salary > 0"
    ) or [{}])[0]

    return {"facets": facets, "salary": salary}


@router.get("/suggest")
def get_suggest(
    filters: Annotated[dict[str, Any], Depends(parse_filters)],
    field: Annotated[str, Query(description="Имя фасета: skills | fields | employer | ...")],
    q_: Annotated[str, Query(alias="q", description="Подстрока для поиска")] = "",
    limit: Annotated[int, Query(ge=1, le=100)] = 50,
) -> dict[str, list[str]]:
    expr = FACET_EXPR.get(field)
    if not expr:
        return {"values": []}
    where = _where_excluding(filters, field)
    rows = execute_query(q.search(_TABLE, where, expr, limit), {"pat": f"%{q_}%"})
    return {"values": [r["v"] for r in rows]}
