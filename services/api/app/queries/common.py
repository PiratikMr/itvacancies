from __future__ import annotations

HAS_SALARY = "salary > 0"

MEDIAN_SALARY = "round(if(isNaN(medianIf(salary, salary > 0)), 0, medianIf(salary, salary > 0)))"

ACTIVE = "is_active = 1"
CLOSED = "is_active = 0"

REMOTE = "has(schedules, 'Удалённо')"

FORMAT_TO_SCHEDULE: dict[str, str] = {
    "remote": "Удалённо",
    "office": "На месте работодателя",
    "hybrid": "Гибрид",
    "travel": "Разъездной",
}


def order_by(sort: str, direction: str, columns: dict[str, str],
             default: str, tiebreak: str = "") -> str:
    col = columns.get(sort, columns[default])
    d = "ASC" if direction == "asc" else "DESC"
    clause = f"{col} {d}"
    return f"{clause}, {tiebreak}" if tiebreak else clause


def where_with(base_where: str, *extra: str) -> str:
    conditions: list[str] = []

    base = base_where.strip()
    if base:
        conditions.append(base[len("WHERE "):].strip() if base.upper().startswith("WHERE ") else base)

    conditions.extend(e for e in extra if e)

    if not conditions:
        return ""
    return "WHERE " + " AND ".join(conditions)
