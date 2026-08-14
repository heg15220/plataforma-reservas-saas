"""Health checks independientes de la disponibilidad del monolito Spring."""

from dataclasses import dataclass

from fastapi import APIRouter
from pydantic import BaseModel, ConfigDict


class HealthResponse(BaseModel):
    """Respuesta estable sin información de infraestructura o secretos."""

    model_config = ConfigDict(extra="forbid")

    status: str
    service: str = "reserly-demand-engine"
    policy_version: str


@dataclass(frozen=True, slots=True)
class RuntimeState:
    """Estado mínimo de readiness; futuros artefactos aprobados ampliarán esta comprobación."""

    policy_version: str = "bootstrap-v1"
    ready: bool = True


def health_router(state: RuntimeState) -> APIRouter:
    """Crea rutas live/ready; no conectan a dependencias del flujo de reserva."""
    router = APIRouter(prefix="/internal/demand/v1/health", tags=["health"])

    @router.get("/live", response_model=HealthResponse)
    async def live() -> HealthResponse:
        return HealthResponse(status="ok", policy_version=state.policy_version)

    @router.get("/ready", response_model=HealthResponse, responses={503: {"description": "Not ready"}})
    async def ready() -> HealthResponse:
        if not state.ready:
            from .errors import DemandEngineError

            raise DemandEngineError("SERVICE_NOT_READY", 503)
        return HealthResponse(status="ready", policy_version=state.policy_version)

    return router
