"""Factoría FastAPI sin efectos laterales para tests y despliegues controlados."""

from __future__ import annotations

import logging
from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .api import internal_api_router
from .affinity import ContentAffinityCalculator
from .config import DemandEngineSettings
from .errors import DemandEngineError
from .embedding_batch import EmbeddingBatchProcessor
from .embeddings import EmbeddingModelManifest, SentenceTransformerEmbedder, TextEmbedder
from .health import RuntimeState, health_router
from .fallback import DeterministicFallback, FallbackPolicy
from .explanations import ExplanationBuilder, ExplanationPolicy
from .middleware import DemandEngineBoundaryMiddleware
from .occupancy import HourlyOccupancyBaseline, OccupancyPolicy
from .demand_aggregation import DemandAggregationPolicy, DemandCapacityCalculator
from .exploration import BasicThompsonSampler, ThompsonPolicy
from .profiles import InMemoryVenueProfileRepository, VenueProfileBuilder
from .session_context import SessionContextBuilder
from .implicit_profiles import ImplicitProfileBuilder, ImplicitProfilePolicy
from .nlp import PersonalCareNlpPipeline, PersonalCareNlpPolicy
from .scoring import ScoreMvp, ScorePolicy


def create_app(
    settings: DemandEngineSettings,
    state: RuntimeState | None = None,
    embedding_embedder: TextEmbedder | None = None,
) -> FastAPI:
    """Construye la aplicación con límites homogéneos y documentación solo si se habilita."""
    logging.basicConfig(level=settings.log_level)
    app = FastAPI(
        title="Reserly Demand Engine (internal)",
        version="0.1.0",
        docs_url="/internal/demand/docs" if settings.docs_enabled else None,
        redoc_url=None,
        openapi_url="/internal/demand/openapi.json" if settings.docs_enabled else None,
    )
    app.state.settings = settings
    app.state.runtime = state or RuntimeState()
    app.state.venue_profiles = InMemoryVenueProfileRepository()
    app.state.venue_profile_builder = VenueProfileBuilder()
    app.state.session_context_builder = SessionContextBuilder()
    app.state.affinity_calculator = ContentAffinityCalculator(settings.embedding_model_promoted)
    policy_root = Path(__file__).resolve().parents[2] / "policies"
    app.state.implicit_profile_builder = ImplicitProfileBuilder(
        ImplicitProfilePolicy.load(policy_root / "implicit-profile.v1.json")
    )
    app.state.nlp_pipeline = PersonalCareNlpPipeline(
        PersonalCareNlpPolicy.load(policy_root / "nlp-personal-care.v1.json")
    )
    app.state.score_mvp = ScoreMvp(
        ScorePolicy.load(policy_root / "score-mvp.v1.json"),
        DeterministicFallback(FallbackPolicy.load(policy_root / "fallback-mvp.v1.json")),
        ExplanationBuilder(ExplanationPolicy.load(policy_root / "explanation-mvp.v1.json")),
    )
    app.state.occupancy_baseline = HourlyOccupancyBaseline(
        OccupancyPolicy.load(policy_root / "occupancy-baseline.v1.json")
    )
    app.state.demand_capacity_calculator = DemandCapacityCalculator(
        DemandAggregationPolicy.load(policy_root / "demand-aggregation.v1.json")
    )
    app.state.thompson_sampler = BasicThompsonSampler(
        ThompsonPolicy.load(policy_root / "thompson-basic.v1.json")
    )
    manifest = EmbeddingModelManifest.load(
        Path(__file__).resolve().parents[2] / "models" / "multilingual-e5-small.v1.json"
    )
    app.state.embedding_batch_processor = EmbeddingBatchProcessor(
        embedding_embedder or SentenceTransformerEmbedder(manifest), manifest.modelKey
    )
    app.add_middleware(DemandEngineBoundaryMiddleware, settings=settings)
    app.include_router(health_router(app.state.runtime))
    app.include_router(internal_api_router(settings))

    @app.exception_handler(DemandEngineError)
    async def expected_error(request: Request, error: DemandEngineError) -> JSONResponse:
        return _error_response(request, error.code, error.status_code)

    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, _: RequestValidationError) -> JSONResponse:
        return _error_response(request, "CONTRACT_INVALID", 422)

    @app.exception_handler(Exception)
    async def unexpected_error(request: Request, _: Exception) -> JSONResponse:
        logging.getLogger("reserly.demand_engine").exception(
            "demand_unexpected requestId=%s", _request_id(request)
        )
        return _error_response(request, "INTERNAL_ERROR", 500)

    return app


def _error_response(request: Request, code: str, status_code: int) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={"code": code, "requestId": _request_id(request)},
    )


def _request_id(request: Request) -> str:
    return getattr(request.state, "request_id", "unavailable")
