from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware

from app.config import settings
from app.database import close_client, init_client
from app.routers import (
    employers,
    facets,
    filters,
    geo,
    meta,
    overview,
    salary,
    skills,
    vacancies,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    init_client()
    logger.info("API started")
    yield
    close_client()
    logger.info("API stopped")


app = FastAPI(
    title="IT Vacancies Analytics API",
    description="Легковесное API для аналитики IT-вакансий из ClickHouse",
    version="0.1.0",
    lifespan=lifespan,
)


app.add_middleware(GZipMiddleware, minimum_size=1024)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

app.include_router(meta.router)
app.include_router(filters.router)
app.include_router(facets.router)
app.include_router(overview.router)
app.include_router(salary.router)
app.include_router(skills.router)
app.include_router(employers.router)
app.include_router(geo.router)
app.include_router(vacancies.router)


@app.get("/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok"}
