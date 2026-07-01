from __future__ import annotations

from datetime import date, timedelta
from typing import Annotated, Any

from fastapi import Query

from app.queries.common import FORMAT_TO_SCHEDULE

_PERIOD_DAYS: dict[str, int] = {"1w": 7, "1m": 30, "3m": 90, "6m": 182, "1y": 365}


def _csv(value: str | None) -> list[str]:
    if not value:
        return []
    return [part.strip() for part in value.split(",") if part.strip()]


def _parse_int(value: str) -> int | None:
    try:
        return int(float(value))
    except (ValueError, TypeError):
        return None


def parse_filters(
    period: Annotated[
        str | None,
        Query(description="Пресет периода публикации: 1w | 1m | 3m | 6m | 1y | all"),
    ] = None,
    date_from: Annotated[date | None, Query(description="Дата публикации с (YYYY-MM-DD)")] = None,
    date_to: Annotated[date | None, Query(description="Дата публикации по (YYYY-MM-DD)")] = None,
    salary: Annotated[
        str | None,
        Query(description="Диапазон зарплаты ₽/мес: 'min,max', напр. 100000,500000 или ,500000"),
    ] = None,
    format: Annotated[
        str | None,
        Query(description="Формат работы: remote | office | hybrid | travel (через запятую)"),
    ] = None,
    platform: Annotated[str | None, Query()] = None,
    employer: Annotated[str | None, Query()] = None,
    currency: Annotated[str | None, Query()] = None,
    experience: Annotated[str | None, Query()] = None,
    skills: Annotated[str | None, Query()] = None,
    schedules: Annotated[str | None, Query()] = None,
    fields: Annotated[str | None, Query()] = None,
    grades: Annotated[str | None, Query()] = None,
    employments: Annotated[str | None, Query()] = None,
    location: Annotated[str | None, Query()] = None,
    country: Annotated[str | None, Query()] = None,
    language: Annotated[str | None, Query()] = None,
    language_level: Annotated[str | None, Query()] = None,
    status: Annotated[str | None, Query(description="Статус: active | closed | all")] = None,
    has_range: Annotated[bool | None, Query(description="Только вакансии с диапазоном зарплат")] = None,
) -> dict[str, Any]:
    filters: dict[str, Any] = {}

    if date_from is not None:
        filters["published_at_from"] = date_from
    elif period and period != "all" and period in _PERIOD_DAYS:
        filters["published_at_from"] = date.today() - timedelta(days=_PERIOD_DAYS[period])
    if date_to is not None:
        filters["published_at_to"] = date_to

    if salary:
        parts = salary.split(",")
        lo = _parse_int(parts[0]) if len(parts) >= 1 else None
        hi = _parse_int(parts[1]) if len(parts) >= 2 else None
        if lo is not None:
            filters["salary_from"] = lo
        if hi is not None:
            filters["salary_to"] = hi

    list_params = {
        "platform": platform, "employer": employer, "currency": currency,
        "experience": experience, "skills": skills, "schedules": schedules,
        "fields": fields, "grades": grades, "employments": employments,
        "location": location, "country": country,
        "language": language, "language_level": language_level,
    }
    for key, raw in list_params.items():
        values = _csv(raw)
        if values:
            filters[key] = values

    fmt_values = [
        FORMAT_TO_SCHEDULE[code]
        for code in _csv(format)
        if code in FORMAT_TO_SCHEDULE
    ]
    if fmt_values:
        merged = set(filters.get("schedules", [])) | set(fmt_values)
        filters["schedules"] = sorted(merged)

    if status in ("active", "closed"):
        filters["status"] = status

    if has_range is not None:
        filters["has_range"] = has_range

    return filters
