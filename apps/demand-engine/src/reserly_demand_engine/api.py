"""Router interno v1; todas sus operaciones exigen identidad de servicio."""

from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Request

from .auth import service_auth_dependency
from .affinity import AffinityRequest, AffinityResponse
from .absa import (
    AbsaEvaluationRequest,
    AbsaEvaluationResponse,
    ReviewAbsaResponse,
    VerifiedReviewRequest,
)
from .config import DemandEngineSettings
from .contracts import (
    ConversionPredictRequest,
    ConversionPredictResponse,
    DeferredDecisionResponse,
    DemandResponse,
    EventsRequest,
    EventsResponse,
    RecommendationRequest,
    Version,
    VenueAttributesResponse,
)
from .errors import DemandEngineError
from .embedding_batch import EmbeddingBatchRequest, EmbeddingBatchResponse
from .profiles import VenueProfileRequest
from .occupancy import OccupancyBaselineRequest, OccupancyBaselineResponse
from .demand_aggregation import (
    DemandAggregationPolicyError,
    DemandAggregationRequest,
    DemandAggregationResponse,
)
from .exploration import (
    ThompsonPolicyError,
    ThompsonSelectionRequest,
    ThompsonSelectionResponse,
    ThompsonUpdateRequest,
    ThompsonUpdateResponse,
)
from .session_context import SessionContextRequest, SessionContextResponse
from .implicit_profiles import ImplicitProfileRequest, ImplicitProfileResponse
from .nlp import NlpAnalyzeRequest, NlpAnalyzeResponse
from .scoring import ScoreMvpRequest, ScoreMvpResponse, ScorePolicyVersionMismatch
from .waitlist_allocation import WaitlistAllocationRequest, WaitlistAllocationResponse
from .smart_promotions import SmartPromotionRequest, SmartPromotionResponse
from .clip_visual_evaluation import ClipVisualEvaluationRequest, ClipVisualEvaluationResponse
from .cross_category_recommendations import CrossCategoryRequest, CrossCategoryResponse


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

    @router.post("/ranking", response_model=ScoreMvpResponse)
    async def rank(body: ScoreMvpRequest, request: Request) -> ScoreMvpResponse:
        """Ordena el conjunto cerrado mediante la política MVP versionada."""
        try:
            return request.app.state.score_mvp.rank(body)
        except ScorePolicyVersionMismatch as error:
            raise DemandEngineError("SCORE_POLICY_VERSION_MISMATCH", 409) from error

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

    @router.post("/profiles/implicit/evaluate", response_model=ImplicitProfileResponse)
    async def evaluate_implicit_profile(
        body: ImplicitProfileRequest, request: Request
    ) -> ImplicitProfileResponse:
        """Calcula preferencias consentidas; Spring conserva la proyección y derechos asociados."""
        try:
            return request.app.state.implicit_profile_builder.build(body)
        except ValueError as error:
            raise DemandEngineError("IMPLICIT_PROFILE_POLICY_INVALID", 409) from error

    @router.post("/nlp/analyze", response_model=NlpAnalyzeResponse)
    async def analyze_personal_care_text(
        body: NlpAnalyzeRequest, request: Request
    ) -> NlpAnalyzeResponse:
        """Extrae conceptos ES/EN en memoria; texto, PII y términos sensibles nunca salen del proceso."""
        try:
            return request.app.state.nlp_pipeline.analyze(body)
        except ValueError as error:
            raise DemandEngineError("NLP_REQUEST_REJECTED", 409) from error

    @router.post("/reviews/absa/analyze", response_model=ReviewAbsaResponse)
    async def analyze_verified_review(
        body: VerifiedReviewRequest, request: Request
    ) -> ReviewAbsaResponse:
        """Analiza una reseña acreditada; Spring persiste solo sus derivados por aspecto."""
        try:
            return request.app.state.review_absa.analyze(body)
        except ValueError as error:
            raise DemandEngineError("ABSA_REQUEST_REJECTED", 409) from error

    @router.post("/reviews/absa/evaluate", response_model=AbsaEvaluationResponse)
    async def evaluate_review_absa(
        body: AbsaEvaluationRequest, request: Request
    ) -> AbsaEvaluationResponse:
        """Calcula métricas agregadas contra una cohorte etiquetada por revisión humana."""
        try:
            return request.app.state.review_absa.evaluate(body)
        except ValueError as error:
            raise DemandEngineError("ABSA_EVALUATION_REJECTED", 409) from error

    @router.post("/affinity/evaluate", response_model=AffinityResponse)
    async def evaluate_affinity(body: AffinityRequest, request: Request) -> AffinityResponse:
        """Evalúa contenido y deja el coseno cerrado hasta promoción explícita."""
        return request.app.state.affinity_calculator.calculate(body)

    @router.post("/occupancy/baseline", response_model=OccupancyBaselineResponse)
    async def calculate_occupancy_baseline(
        body: OccupancyBaselineRequest, request: Request
    ) -> OccupancyBaselineResponse:
        """Calcula un baseline día-hora sin acceder a reservas ni persistir datos."""
        return request.app.state.occupancy_baseline.calculate(body)

    @router.post("/demand/aggregate", response_model=DemandAggregationResponse)
    async def aggregate_demand(
        body: DemandAggregationRequest, request: Request
    ) -> DemandAggregationResponse:
        """Calcula gaps agregados y suprime conteos que no alcanzan privacidad mínima."""
        try:
            return request.app.state.demand_capacity_calculator.calculate(body)
        except DemandAggregationPolicyError as error:
            raise DemandEngineError("DEMAND_PERIOD_INVALID", 422) from error

    @router.post("/exploration/select", response_model=ThompsonSelectionResponse)
    async def select_exploration(
        body: ThompsonSelectionRequest, request: Request
    ) -> ThompsonSelectionResponse:
        """Selecciona una cuota acotada después de restricciones y calidad."""
        try:
            return request.app.state.thompson_sampler.select(body)
        except ThompsonPolicyError as error:
            raise DemandEngineError("THOMPSON_POLICY_INVALID", 409) from error

    @router.post("/exploration/update", response_model=ThompsonUpdateResponse)
    async def update_exploration(
        body: ThompsonUpdateRequest, request: Request
    ) -> ThompsonUpdateResponse:
        """Aplica un outcome una sola vez; Spring persiste state y ledger atómicamente."""
        try:
            return request.app.state.thompson_sampler.update(body)
        except ThompsonPolicyError as error:
            raise DemandEngineError("THOMPSON_UPDATE_REJECTED", 409) from error

    @router.post("/waitlist/allocate", response_model=WaitlistAllocationResponse)
    async def allocate_waitlist(
        body: WaitlistAllocationRequest, request: Request
    ) -> WaitlistAllocationResponse:
        """Propone ofertas escalonadas; Spring conserva persistencia, emisión y aceptación."""
        try:
            return request.app.state.waitlist_allocator.allocate(body)
        except ValueError as error:
            raise DemandEngineError("WAITLIST_ALLOCATION_REJECTED", 409) from error

    @router.post("/promotions/plan", response_model=SmartPromotionResponse)
    async def plan_promotions(
        body: SmartPromotionRequest, request: Request
    ) -> SmartPromotionResponse:
        """Propone promociones preaprobadas sin emitir contacto, cupón ni mutación transaccional."""
        try:
            return request.app.state.smart_promotion_planner.plan(body)
        except ValueError as error:
            raise DemandEngineError("SMART_PROMOTION_REJECTED", 409) from error

    @router.post("/visual/clip/evaluate", response_model=ClipVisualEvaluationResponse)
    async def evaluate_clip_visuals(
        body: ClipVisualEvaluationRequest, request: Request
    ) -> ClipVisualEvaluationResponse:
        """Evalúa embeddings visuales autorizados sin recibir píxeles, EXIF ni personas."""
        try:
            return request.app.state.clip_visual_evaluator.evaluate(body)
        except ValueError as error:
            raise DemandEngineError("CLIP_VISUAL_EVALUATION_REJECTED", 409) from error

    @router.post("/recommendations/cross-category", response_model=CrossCategoryResponse)
    async def recommend_cross_category(
        body: CrossCategoryRequest, request: Request
    ) -> CrossCategoryResponse:
        """Cruza categorías por intención explícita sin inferir un perfil personal persistente."""
        try:
            return request.app.state.cross_category_recommender.recommend(body)
        except ValueError as error:
            raise DemandEngineError("CROSS_CATEGORY_RECOMMENDATION_REJECTED", 409) from error

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
