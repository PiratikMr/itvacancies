from __future__ import annotations

import logging
import threading
import time
from typing import Any

import clickhouse_connect
from clickhouse_connect.driver.client import Client

from app.config import settings

logger = logging.getLogger(__name__)

_client: Client | None = None

# Lightweight in-process result cache. The dataset only changes once per ETL
# run, so identical queries within a short window can be served from memory
# instead of re-hitting ClickHouse. Callers MUST treat results as read-only.
_CACHE_TTL = 120.0  # seconds
_CACHE_MAX = 256
_cache: dict[str, tuple[float, list[dict[str, Any]]]] = {}
_cache_lock = threading.Lock()


def init_client() -> None:
    global _client
    _client = clickhouse_connect.get_client(
        host=settings.ch_host,
        port=settings.ch_port,
        username=settings.ch_user,
        password=settings.ch_pass,
        database=settings.ch_db,
        autogenerate_session_id=False,
    )
    logger.info(
        "ClickHouse client connected: %s:%s/%s",
        settings.ch_host,
        settings.ch_port,
        settings.ch_db,
    )


def close_client() -> None:
    global _client
    if _client is not None:
        _client.close()
        _client = None
        logger.info("ClickHouse client closed")


def get_client() -> Client:
    if _client is None:
        raise RuntimeError("ClickHouse client is not initialized")
    return _client


def execute_query(
    query: str,
    parameters: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    key = query if parameters is None else f"{query}\x00{sorted(parameters.items())}"
    now = time.monotonic()

    with _cache_lock:
        hit = _cache.get(key)
        if hit is not None and now - hit[0] < _CACHE_TTL:
            return hit[1]

    client = get_client()
    result = client.query(query, parameters=parameters)
    columns = result.column_names
    rows = [dict(zip(columns, row)) for row in result.result_rows]

    with _cache_lock:
        if len(_cache) >= _CACHE_MAX:
            oldest = min(_cache, key=lambda k: _cache[k][0])
            _cache.pop(oldest, None)
        _cache[key] = (now, rows)

    return rows
