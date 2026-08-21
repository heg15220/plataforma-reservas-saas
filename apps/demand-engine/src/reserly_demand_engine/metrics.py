"""Métricas Prometheus de cardinalidad cerrada para el motor de demanda.

Ningún label acepta identificadores, URLs libres, versiones arbitrarias, texto o datos personales.
Los jobs y servicios llaman métodos semánticos; el registro valida allowlists y límites antes de
publicar. Cada aplicación recibe su propio CollectorRegistry para aislar tests y procesos.
"""

from __future__ import annotations

import math
from typing import Literal

from fastapi import APIRouter, Response
from prometheus_client import CONTENT_TYPE_LATEST, CollectorRegistry, Counter, Gauge, Histogram
from prometheus_client.exposition import generate_latest


RankingStatus = Literal["ranked", "fallback_ranked", "no_eligible_candidates"]
ModelStage = Literal["candidate", "shadow", "canary", "champion"]


class DemandMetrics:
    """Registro documentado que protege tipos, unidades y dimensiones permitidas."""

    _routes = {
        "/internal/demand/v1/events", "/internal/demand/v1/ranking",
        "/internal/demand/v1/recommendations", "/internal/demand/v1/conversion/predict",
        "/internal/demand/v1/embeddings/generate", "/internal/demand/v1/session/context",
        "/internal/demand/v1/profiles/implicit/evaluate", "/internal/demand/v1/nlp/analyze",
        "/internal/demand/v1/reviews/absa/analyze", "/internal/demand/v1/reviews/absa/evaluate",
        "/internal/demand/v1/affinity/evaluate", "/internal/demand/v1/occupancy/baseline",
        "/internal/demand/v1/demand/aggregate", "/internal/demand/v1/exploration/select",
        "/internal/demand/v1/exploration/update", "/internal/demand/v1/waitlist/allocate",
        "/internal/demand/v1/promotions/plan", "/internal/demand/v1/visual/clip/evaluate",
        "/internal/demand/v1/recommendations/cross-category",
        "/internal/demand/v1/learning/incremental/evaluate",
        "/internal/demand/v1/analytics/incrementality/evaluate",
        "/internal/demand/v1/demand/{venue_id}",
        "/internal/demand/v1/venues/{venue_id}/attributes",
        "/internal/demand/v1/venues/{venue_id}/attributes/evaluate",
        "/internal/demand/v1/health/live", "/internal/demand/v1/health/ready",
        "/internal/demand/v1/metrics",
    }
    _surfaces = {"ranking", "recommendation", "exploration", "incrementality", "overall"}
    _cohorts = {"newVenue", "establishedVenue", "overall"}
    _value_kinds = {"attributedNetRevenue", "incrementalNetRevenue", "activationCost"}
    _model_families = {"ranking", "conversion", "forecast", "uplift", "embedding"}

    def __init__(self, registry: CollectorRegistry | None = None) -> None:
        self.registry = registry or CollectorRegistry(auto_describe=True)
        self.http_requests = Counter(
            "reserly_demand_http_requests_total", "Solicitudes HTTP internas por ruta normalizada.",
            ("method", "route", "status_class"), registry=self.registry,
        )
        self.http_duration = Histogram(
            "reserly_demand_http_request_duration_seconds",
            "Latencia total del límite HTTP interno en segundos.", ("route",),
            buckets=(0.005, 0.01, 0.025, 0.05, 0.1, 0.15, 0.25, 0.5, 1.0, 2.5, 5.0),
            registry=self.registry,
        )
        self.ingestion_events = Counter(
            "reserly_demand_ingestion_events_total", "Eventos de demanda por resultado cerrado.",
            ("outcome",), registry=self.registry,
        )
        self.ranking_requests = Counter(
            "reserly_demand_ranking_requests_total", "Rankings por resultado gobernado.",
            ("status",), registry=self.registry,
        )
        self.fallbacks = Counter(
            "reserly_demand_fallback_total", "Fallbacks aplicados por razón acotada.",
            ("reason",), registry=self.registry,
        )
        self.model_errors = Counter(
            "reserly_demand_model_errors_total", "Errores de modelo por etapa y familia.",
            ("stage", "family"), registry=self.registry,
        )
        self.score_distribution = Histogram(
            "reserly_demand_score", "Distribución de scores gobernados entre cero y uno.",
            ("stage",), buckets=(0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0),
            registry=self.registry,
        )
        self.drift_psi = Gauge(
            "reserly_demand_drift_psi", "PSI agregado más adverso por etapa del modelo.",
            ("stage",), registry=self.registry,
        )
        self.calibration_error = Gauge(
            "reserly_demand_calibration_error_ratio", "Error de calibración por etapa y segmento.",
            ("stage", "segment"), registry=self.registry,
        )
        self.coverage = Gauge(
            "reserly_demand_coverage_ratio", "Cobertura agregada de candidatos o outcomes.",
            ("surface",), registry=self.registry,
        )
        self.diversity = Gauge(
            "reserly_demand_diversity_ratio", "Diversidad agregada de resultados elegibles.",
            ("surface",), registry=self.registry,
        )
        self.exposure = Gauge(
            "reserly_demand_exposure_ratio", "Cuota agregada de exposición por cohorte operativa.",
            ("cohort",), registry=self.registry,
        )
        self.business_value = Gauge(
            "reserly_demand_business_value_eur", "Valor comercial agregado de la última ventana EUR.",
            ("kind",), registry=self.registry,
        )
        self.rollout_traffic = Gauge(
            "reserly_demand_rollout_traffic_ratio", "Cuota vigente de tráfico por etapa.",
            ("stage",), registry=self.registry,
        )
        self._initialize_series()

    def _initialize_series(self) -> None:
        """Publica ceros conocidos para que alertas no confundan ausencia con normalidad."""
        for outcome in ("accepted", "rejected", "duplicate"):
            self.ingestion_events.labels(outcome)
        for status in ("ranked", "fallback_ranked", "no_eligible_candidates"):
            self.ranking_requests.labels(status)
        for stage in ("candidate", "shadow", "canary", "champion"):
            self.drift_psi.labels(stage).set(0)
            self.rollout_traffic.labels(stage).set(0)

    def observe_http(self, method: str, route: str | None, status: int, seconds: float) -> None:
        """Observa una respuesta usando únicamente método, plantilla y clase acotados."""
        normalized = route if route in self._routes else "unmatched"
        normalized_method = method if method in {"GET", "POST"} else "OTHER"
        status_class = f"{status // 100}xx" if 100 <= status <= 599 else "unknown"
        self.http_requests.labels(normalized_method, normalized, status_class).inc()
        self.http_duration.labels(normalized).observe(max(seconds, 0.0))

    def record_ingestion(self, outcome: Literal["accepted", "rejected", "duplicate"], count: int) -> None:
        """Incrementa un outcome agregado sin inspeccionar ni conservar eventos."""
        if count < 0:
            raise ValueError("METRICS_COUNT_INVALID")
        self.ingestion_events.labels(outcome).inc(count)

    def record_ranking(self, status: RankingStatus, candidate_count: int, eligible_count: int,
                       scores: list[float], fallback_reason: str | None) -> None:
        """Publica resultado, cobertura, scores y fallback de un ranking ya gobernado."""
        if candidate_count < 1 or not 0 <= eligible_count <= candidate_count:
            raise ValueError("METRICS_RANKING_COUNT_INVALID")
        self.ranking_requests.labels(status).inc()
        self.coverage.labels("ranking").set(eligible_count / candidate_count)
        for score in scores:
            self._ratio(score)
            self.score_distribution.labels("champion").observe(score)
        if status != "ranked":
            reason = fallback_reason if fallback_reason in {
                "model_not_available", "model_timeout", "dependency_unavailable",
                "insufficient_candidates", "policy_disabled", "unknown",
            } else "unknown"
            self.fallbacks.labels(reason).inc()

    def set_model_health(self, stage: ModelStage, *, drift_psi: float,
                         calibration_error: float, segment: Literal["overall", "es", "en"]) -> None:
        """Actualiza la última evaluación agregada de drift y calibración."""
        if not math.isfinite(drift_psi) or drift_psi < 0:
            raise ValueError("METRICS_DRIFT_INVALID")
        self._ratio(calibration_error)
        self.drift_psi.labels(stage).set(drift_psi)
        self.calibration_error.labels(stage, segment).set(calibration_error)

    def set_population_health(self, *, surface: str, coverage: float, diversity: float,
                              new_venue_exposure: float) -> None:
        """Actualiza salud poblacional solo cuando existen las tres evidencias agregadas."""
        if surface not in self._surfaces:
            raise ValueError("METRICS_SURFACE_INVALID")
        for value in (coverage, diversity, new_venue_exposure):
            self._ratio(value)
        self.coverage.labels(surface).set(coverage)
        self.diversity.labels(surface).set(diversity)
        self.exposure.labels("newVenue").set(new_venue_exposure)
        self.exposure.labels("establishedVenue").set(1.0 - new_venue_exposure)

    def set_coverage(self, surface: str, coverage: float) -> None:
        """Actualiza cobertura aislada cuando no existe evidencia de diversidad/exposición."""
        if surface not in self._surfaces:
            raise ValueError("METRICS_SURFACE_INVALID")
        self._ratio(coverage)
        self.coverage.labels(surface).set(coverage)

    def set_business_value(self, kind: str, value_eur: float) -> None:
        """Publica el valor agregado de ventana para una clase comercial permitida."""
        if kind not in self._value_kinds or not math.isfinite(value_eur):
            raise ValueError("METRICS_BUSINESS_VALUE_INVALID")
        self.business_value.labels(kind).set(value_eur)

    def set_rollout_traffic(self, stage: ModelStage, ratio: float) -> None:
        """Publica la cuota efectiva de una etapa de rollout."""
        self._ratio(ratio)
        self.rollout_traffic.labels(stage).set(ratio)

    def record_model_error(self, stage: ModelStage, family: str) -> None:
        """Incrementa un error de modelo sin admitir versión, excepción o texto como label."""
        if family not in self._model_families:
            raise ValueError("METRICS_MODEL_FAMILY_INVALID")
        self.model_errors.labels(stage, family).inc()

    @staticmethod
    def _ratio(value: float) -> None:
        if not math.isfinite(value) or not 0.0 <= value <= 1.0:
            raise ValueError("METRICS_RATIO_INVALID")


def metrics_router(metrics: DemandMetrics) -> APIRouter:
    """Expone únicamente texto Prometheus por red interna; nunca serializa estado de negocio."""
    router = APIRouter()

    @router.get("/internal/demand/v1/metrics", include_in_schema=False)
    async def scrape() -> Response:
        return Response(generate_latest(metrics.registry), media_type=CONTENT_TYPE_LATEST)

    return router
