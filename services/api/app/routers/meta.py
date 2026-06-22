from __future__ import annotations

from typing import Any

from fastapi import APIRouter

from app.config import settings
from app.database import execute_query

router = APIRouter(prefix="/api/v1", tags=["meta"])


@router.get("/meta")
def get_meta() -> dict[str, Any]:
    rows = execute_query(
        f"SELECT finished_at, usd_rate, eur_rate "
        f"FROM {settings.ch_db}.meta ORDER BY finished_at DESC LIMIT 1"
    )
    row = rows[0] if rows else {}
    usd = row.get("usd_rate") or 0
    eur = row.get("eur_rate") or 0
    return {
        "data_updated_at": row.get("finished_at"),
        "fx": {"usd": usd, "eur": eur} if (usd and eur) else None,
    }
