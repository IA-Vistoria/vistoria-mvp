"""Simple API-key gate for public exposure of /analyze."""

from __future__ import annotations

import hmac
import logging

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.config import get_settings

logger = logging.getLogger(__name__)

# Paths that stay reachable without API key (probes / docs / static tester).
PUBLIC_PATH_PREFIXES = (
    "/health",
    "/docs",
    "/openapi.json",
    "/redoc",
    "/static",
)


class ApiKeyMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        settings = get_settings()
        path = request.url.path

        if not settings.require_api_key:
            return await call_next(request)

        if any(path == p or path.startswith(p + "/") for p in PUBLIC_PATH_PREFIXES):
            return await call_next(request)

        if not settings.api_key:
            logger.error("REQUIRE_API_KEY is on but API_KEY is empty")
            return JSONResponse(
                status_code=503,
                content={"detail": "API key is not configured on the server"},
            )

        provided = request.headers.get("X-API-Key") or ""
        if not provided:
            auth = request.headers.get("Authorization") or ""
            if auth.lower().startswith("bearer "):
                provided = auth[7:].strip()

        if not provided or not hmac.compare_digest(provided, settings.api_key):
            return JSONResponse(status_code=401, content={"detail": "Invalid or missing API key"})

        return await call_next(request)
