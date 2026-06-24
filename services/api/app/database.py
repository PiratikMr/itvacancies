from __future__ import annotations

import logging
from typing import Any

import clickhouse_connect
from clickhouse_connect.driver.client import Client

from app.config import settings

logger = logging.getLogger(__name__)

_client: Client | None = None


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
    client = get_client()
    result = client.query(query, parameters=parameters)
    columns = result.column_names
    return [dict(zip(columns, row)) for row in result.result_rows]
