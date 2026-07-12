from __future__ import annotations

HAS_SALARY = "salary > 0"

MEDIAN_SALARY = "round(if(isNaN(medianIf(salary, salary > 0)), 0, medianIf(salary, salary > 0)))"

ACTIVE = "is_active = 1"
CLOSED = "is_active = 0"

REMOTE = "has(schedules, 'Удалённо')"

SYNTHETIC_CLOSE_PLATFORMS = ("Adzuna",)
_synthetic = ", ".join(f"'{p}'" for p in SYNTHETIC_CLOSE_PLATFORMS)

REAL_CLOSED = f"{CLOSED} AND closed_at >= published_at AND platform NOT IN ({_synthetic})"


def avg_close_days_expr(predicate: str = REAL_CLOSED) -> str:
    expr = f"avgIf(dateDiff('day', published_at, closed_at), {predicate})"
    return f"if(isNaN({expr}), NULL, round({expr}))"

FORMAT_TO_SCHEDULE: dict[str, str] = {
    "remote": "Удалённо",
    "office": "На месте работодателя",
    "hybrid": "Гибрид",
    "travel": "Разъездной",
}


def order_by(sort: str, direction: str, columns: dict[str, str],
             default: str, tiebreak: str = "",
             empties: dict[str, str] | None = None) -> str:
    key = sort if sort in columns else default
    col = columns[key]
    d = "ASC" if direction == "asc" else "DESC"

    parts: list[str] = []
    empty_expr = (empties or {}).get(key)
    if empty_expr:
        parts.append(f"({empty_expr}) ASC")
    parts.append(f"{col} {d}")
    if tiebreak:
        parts.append(tiebreak)
    return ", ".join(parts)


def where_with(base_where: str, *extra: str) -> str:
    conditions: list[str] = []

    base = base_where.strip()
    if base:
        conditions.append(base[len("WHERE "):].strip() if base.upper().startswith("WHERE ") else base)

    conditions.extend(e for e in extra if e)

    if not conditions:
        return ""
    return "WHERE " + " AND ".join(conditions)
