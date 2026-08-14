"""Límites de cuerpo, timeout total, correlación y logging minimizado."""

from __future__ import annotations

import asyncio
import logging
import time
from uuid import UUID, uuid4

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.responses import Response

from .config import DemandEngineSettings

LOGGER = logging.getLogger("reserly.demand_engine.http")


class DemandEngineBoundaryMiddleware(BaseHTTPMiddleware):
    """Aplica guardrails antes de ejecutar lógica funcional y nunca registra el body."""

    def __init__(self, app: object, settings: DemandEngineSettings) -> None:
        super().__init__(app)  # type: ignore[arg-type]
        self._settings = settings

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        request_id = _request_id(request.headers.get("X-Reserly-Correlation-Id"))
        request.state.request_id = request_id
        content_length = request.headers.get("content-length")
        if content_length is not None:
            try:
                if int(content_length) > self._settings.maximum_request_bytes:
                    return _error("REQUEST_TOO_LARGE", 413, request_id)
            except ValueError:
                return _error("CONTRACT_INVALID", 422, request_id)

        started = time.perf_counter()
        try:
            async with asyncio.timeout(self._settings.request_timeout_seconds):
                response = await call_next(request)
        except TimeoutError:
            response = _error("REQUEST_TIMEOUT", 504, request_id)
        duration_ms = (time.perf_counter() - started) * 1_000
        response.headers["X-Reserly-Correlation-Id"] = request_id
        response.headers["X-Content-Type-Options"] = "nosniff"
        LOGGER.info(
            "demand_request requestId=%s method=%s path=%s status=%s durationMs=%.3f",
            request_id,
            request.method,
            request.url.path,
            response.status_code,
            duration_ms,
        )
        return response


def _request_id(raw: str | None) -> str:
    try:
        return str(UUID(raw)) if raw is not None else str(uuid4())
    except (ValueError, AttributeError):
        return str(uuid4())


def _error(code: str, status_code: int, request_id: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={"code": code, "requestId": request_id},
    )
