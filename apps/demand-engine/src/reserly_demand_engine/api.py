"""Router interno v1; todas sus operaciones exigen identidad de servicio."""

from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Request

from .auth import service_auth_dependency
from .affinity import AffinityRequest, AffinityResponse
from .config import DemandEngineSettings
from .contracts import (
    ConversionPredictRequest,
    ConversionPredictResponse,
    DeferredDecisionResponse,
    DemandResponse,
    EventsRequest,
    EventsResponse,
    RecommendationRequest,
    RankingRequest,
    Version,
    VenueAttributesResponse,
)
from .errors import DemandEngineError
from .embedding_batch import EmbeddingBatchRequest, EmbeddingBatchResponse
from .profiles import VenueProfileRequest
from .session_context import SessionContextRequest, SessionContextResponse


def internal_api_router(settings: DemandEngineSettings) -> APIRouter:
    """Construye la API privada y aplica autenticación una vez al router completo."""
    router = APIRouter(
        prefix="/internal/demand/v1",
        tags=["internal-demand"],
        dependencies=[Depends(service_auth_dependency(settings))],
    )

    @router.post("/events", response_model=EventsResponse)
    async def validate_events(body: EventsRequest) -> EventsResponse:
        """Valida eventos para consumidores internos; la persistencia canónica sigue en Spring."""
        return EventsResponse(
            requestId=body.requestId,
            policyVersion=body.policyVersion,
            validatedCount=len(body.events),
        )

    @router.post("/recommendations", response_model=DeferredDecisionResponse)
    async def recommend(body: RecommendationRequest) -> DeferredDecisionResponse:
        """Fuerza fallback hasta que tareas posteriores instalen generación y scoring."""
        return _deferred(body.requestId, body.policyVersion, len(body.candidates))

    @router.post("/ranking", response_model=DeferredDecisionResponse)
    async def rank(body: RankingRequest) -> DeferredDecisionResponse:
        """No reordena ni amplía candidatos mientras no exista una política aprobada."""
        return _deferred(body.requestId, body.policyVersion, len(body.candidates))

    @router.get("/venues/{venue_id}/attributes", response_model=VenueAttributesResponse)
    async def venue_attributes(venue_id: UUID, request: Request) -> VenueAttributesResponse:
        """Lee un perfil calculado; 20.3 instalará el repositorio de proyecciones."""
        profile = request.app.state.venue_profiles.get(venue_id)
        if profile is None:
            raise DemandEngineError("VENUE_PROFILE_NOT_FOUND", 404)
        return profile.model_copy(update={"requestId": UUID(request.state.request_id)})

    @router.post(
        "/venues/{venue_id}/attributes/evaluate",
        response_model=VenueAttributesResponse,
    )
    async def evaluate_venue_attributes(
        venue_id: UUID,
        body: VenueProfileRequest,
        request: Request,
    ) -> VenueAttributesResponse:
        """Calcula una proyección interpretable; Spring conserva la persistencia autoritativa."""
        if venue_id != body.venueId:
            raise DemandEngineError("VENUE_ID_MISMATCH", 409)
        profile = request.app.state.venue_profile_builder.build(body)
        request.app.state.venue_profiles.put(profile)
        return profile

    @router.post("/conversion/predict", response_model=ConversionPredictResponse)
    async def predict_conversion(body: ConversionPredictRequest) -> ConversionPredictResponse:
        """Declara modelo ausente para que Spring use su baseline determinista."""
        return ConversionPredictResponse(
            requestId=body.requestId,
            policyVersion=body.policyVersion,
        )

    @router.post("/embeddings/generate", response_model=EmbeddingBatchResponse)
    async def generate_embeddings(body: EmbeddingBatchRequest, request: Request) -> EmbeddingBatchResponse:
        """Calcula un lote en sombra; Spring conserva la única escritura a pgvector."""
        return request.app.state.embedding_batch_processor.generate(body)

    @router.post("/session/context", response_model=SessionContextResponse)
    async def build_session_context(
        body: SessionContextRequest, request: Request
    ) -> SessionContextResponse:
        """Construye un perfil efímero y limita señales cuando falta consentimiento."""
        return request.app.state.session_context_builder.build(body)

    @router.post("/affinity/evaluate", response_model=AffinityResponse)
    async def evaluate_affinity(body: AffinityRequest, request: Request) -> AffinityResponse:
        """Evalúa contenido y deja el coseno cerrado hasta promoción explícita."""
        return request.app.state.affinity_calculator.calculate(body)

    @router.get("/demand/{venue_id}", response_model=DemandResponse)
    async def venue_demand(
        venue_id: UUID,
        request: Request,
        policyVersion: Version = "demand-bootstrap-v1",
    ) -> DemandResponse:
        """Declara baseline ausente sin fabricar una estimación de demanda."""
        return DemandResponse(
            requestId=UUID(request.state.request_id),
            venueId=venue_id,
            policyVersion=policyVersion,
        )

    return router


def _deferred(request_id: UUID, policy_version: str, count: int) -> DeferredDecisionResponse:
    """Centraliza la respuesta temporal y mantiene semántica idéntica en ambos endpoints."""
    return DeferredDecisionResponse(
        requestId=request_id,
        policyVersion=policy_version,
        candidateCount=count,
    )
