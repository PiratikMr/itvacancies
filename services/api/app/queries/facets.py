from __future__ import annotations

FACET_EXPR: dict[str, str] = {
    "platform": "platform",
    "employer": "employer",
    "currency": "currency",
    "experience": "experience",
    "grades": "arrayJoin(grades)",
    "fields": "arrayJoin(fields)",
    "skills": "arrayJoin(skills)",
    "employments": "arrayJoin(employments)",
}


def options(table: str, where: str, expr: str, limit: int = 50) -> str:
    return f"""
        SELECT {expr} AS v, count() AS c
        FROM {table}
        {where}
        GROUP BY v
        HAVING v != ''
        ORDER BY c DESC
        LIMIT {limit}
    """


def search(table: str, where: str, expr: str, limit: int = 50) -> str:
    return f"""
        SELECT {expr} AS v, count() AS c
        FROM {table}
        {where}
        GROUP BY v
        HAVING v ILIKE {{pat:String}}
        ORDER BY c DESC
        LIMIT {limit}
    """
