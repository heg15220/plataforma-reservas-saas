"""Puerta reproducible para promover ranking de shadow a piloto o de piloto a rollout."""

from __future__ import annotations

import json
import math
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class SampleRequirement(BaseModel):
    """Muestra mínima; no sustituye el análisis de potencia del efecto primario."""

    model_config = ConfigDict(extra="forbid")

    minimumConsecutiveDays: int = Field(ge=1)
    minimumSessionsPerVariant: int = Field(ge=0)
    minimumCompletedBookings: int = Field(ge=0)


class MetricGate(BaseModel):
    """Definición auditable y umbral por etapa de una única métrica agregada."""

    model_config = ConfigDict(extra="forbid")

    metricKey: str = Field(pattern=r"^[A-Za-z][A-Za-z0-9]{1,63}$")
    phase: Literal["offline", "shadow", "online", "experiment", "guardrail"]
    definition: str = Field(min_length=10, max_length=500)
    numerator: str = Field(min_length=3, max_length=300)
    denominator: str = Field(min_length=3, max_length=300)
    unit: Literal["ratio", "milliseconds", "count", "eur", "eur_per_booking"]
    direction: Literal["minimum", "maximum"]
    shadowToPilotThreshold: float | None = None
    pilotToRolloutThreshold: float | None = None
    zeroTolerance: bool = False

    @model_validator(mode="after")
    def validate_thresholds(self) -> "MetricGate":
        """Impide métricas sin uso y guardrails zero-tolerance con umbral distinto de cero."""
        thresholds = (self.shadowToPilotThreshold, self.pilotToRolloutThreshold)
        if all(value is None for value in thresholds) or any(
            value is not None and not math.isfinite(value) for value in thresholds
        ):
            raise ValueError("PROMOTION_METRIC_THRESHOLD_INVALID")
        if self.zeroTolerance and any(value not in (None, 0.0) for value in thresholds):
            raise ValueError("PROMOTION_ZERO_TOLERANCE_INVALID")
        return self


class PromotionPolicy(BaseModel):
    """Contrato inmutable de métricas, dataset, baseline, muestra y confianza."""

    model_config = ConfigDict(extra="forbid")

    policyVersion: str
    datasetVersion: str
    baselineVersion: str
    requiredDataValidationPolicyVersion: str
    confidenceLevel: float = Field(gt=0.0, lt=1.0)
    sampleRequirements: dict[Literal["shadowToPilot", "pilotToRollout"], SampleRequirement]
    metrics: list[MetricGate] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_registry(self) -> "PromotionPolicy":
        """Rechaza claves duplicadas y políticas que omiten una de las dos etapas."""
        keys = [metric.metricKey for metric in self.metrics]
        if len(keys) != len(set(keys)) or set(self.sampleRequirements) != {
            "shadowToPilot",
            "pilotToRollout",
        }:
            raise ValueError("PROMOTION_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "PromotionPolicy":
        """Carga JSON estricto; un cambio material debe usar otro nombre/versionado."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class BaselineArtifact(BaseModel):
    """Fotografía offline de referencia; declara si contiene evidencia productiva."""

    model_config = ConfigDict(extra="forbid")

    baselineVersion: str
    datasetVersion: str
    capturedAt: str
    provenance: str
    productionEvidence: bool
    metrics: dict[str, float]
    limitations: list[str] = Field(min_length=1)

    @classmethod
    def load(cls, path: Path) -> "BaselineArtifact":
        """Carga un baseline agregado sin ejemplos, identidades ni features individuales."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class DatasetSplitPolicy(BaseModel):
    """Declara separación temporal obligatoria cuando el dataset deje de ser sintético."""

    model_config = ConfigDict(extra="forbid")

    training: str
    evaluation: str
    futureProductionRule: str


class DatasetPrivacy(BaseModel):
    """Manifiesto de procedencia y allowlist de un dataset evaluable."""

    model_config = ConfigDict(extra="forbid")

    containsProductionData: bool
    containsPersonalData: bool
    allowedFields: list[str]


class RankingEvaluationCase(BaseModel):
    """Caso canónico minimizado; usa códigos ficticios y expectativas observables."""

    model_config = ConfigDict(extra="forbid")

    caseId: str = Field(pattern=r"^[a-z0-9][a-z0-9-]{2,63}$")
    locale: Literal["es", "en"]
    categoryCode: Literal["peluqueria", "centro-de-estetica"]
    activeFilters: list[str]
    expectedTopCandidateId: str | None
    hardExcludedCandidateIds: list[str]
    expectedExplanationCodes: list[str]


class RankingEvaluationDataset(BaseModel):
    """Dataset sintético cerrado para regresión; no es material de entrenamiento."""

    model_config = ConfigDict(extra="forbid")

    datasetVersion: str
    scope: Literal["synthetic-personal-care-contract"]
    locales: list[Literal["es", "en"]]
    verticals: list[Literal["peluqueria", "centro-de-estetica"]]
    splitPolicy: DatasetSplitPolicy
    privacy: DatasetPrivacy
    cases: list[RankingEvaluationCase] = Field(min_length=12)

    @model_validator(mode="after")
    def validate_governance(self) -> "RankingEvaluationDataset":
        """Impide PII/producción, casos duplicados o ampliar silenciosamente el contrato."""
        expected_fields = {
            "caseId",
            "locale",
            "categoryCode",
            "activeFilters",
            "expectedTopCandidateId",
            "hardExcludedCandidateIds",
            "expectedExplanationCodes",
        }
        case_ids = [case.caseId for case in self.cases]
        if (
            self.privacy.containsProductionData
            or self.privacy.containsPersonalData
            or set(self.privacy.allowedFields) != expected_fields
            or len(case_ids) != len(set(case_ids))
            or set(self.locales) != {"es", "en"}
            or set(self.verticals) != {"peluqueria", "centro-de-estetica"}
        ):
            raise ValueError("PROMOTION_DATASET_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "RankingEvaluationDataset":
        """Carga el dataset completo y comprueba su manifiesto antes de evaluar métricas."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class PromotionSnapshot(BaseModel):
    """Evidencia agregada; exige el token content-addressed de validación pre-promoción."""

    model_config = ConfigDict(extra="forbid")

    snapshotVersion: Literal[1]
    targetStage: Literal["shadowToPilot", "pilotToRollout"]
    policyVersion: str
    datasetVersion: str
    baselineVersion: str
    dataValidationPolicyVersion: str
    dataValidationEvidenceSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    dataValidationPassed: Literal[True]
    consecutiveDays: int = Field(ge=0)
    sessionsByVariant: dict[str, int]
    completedBookings: int = Field(ge=0)
    poweredSample: bool
    confidenceLevel: float = Field(gt=0.0, lt=1.0)
    metricValues: dict[str, float]

    @model_validator(mode="after")
    def validate_values(self) -> "PromotionSnapshot":
        """Rechaza muestras negativas, variantes sin código y NaN/infinitos."""
        if any(not key or value < 0 for key, value in self.sessionsByVariant.items()):
            raise ValueError("PROMOTION_SAMPLE_INVALID")
        if any(not math.isfinite(value) for value in self.metricValues.values()):
            raise ValueError("PROMOTION_METRIC_VALUE_INVALID")
        return self


@dataclass(frozen=True, slots=True)
class GateResult:
    """Evidencia legible de una comprobación individual."""

    gate: str
    passed: bool
    observed: float | int | bool | str
    required: float | int | bool | str


@dataclass(frozen=True, slots=True)
class PromotionDecision:
    """Decisión completa: solo promotable cuando todas las puertas requeridas pasan."""

    policyVersion: str
    targetStage: str
    promotable: bool
    gateResults: tuple[GateResult, ...]

    def as_dict(self) -> dict[str, object]:
        """Serializa sin incluir datos distintos de métricas y requisitos agregados."""
        return {
            "policyVersion": self.policyVersion,
            "targetStage": self.targetStage,
            "promotable": self.promotable,
            "gateResults": [asdict(result) for result in self.gateResults],
        }


def evaluate_promotion(
    policy: PromotionPolicy,
    baseline: BaselineArtifact,
    snapshot: PromotionSnapshot,
) -> PromotionDecision:
    """Evalúa versiones, muestra, potencia, umbrales y no-regresión offline; falla cerrado."""
    _validate_versions(policy, baseline, snapshot)
    known_metrics = {metric.metricKey for metric in policy.metrics}
    unknown = set(snapshot.metricValues) - known_metrics
    if unknown:
        raise ValueError("PROMOTION_UNKNOWN_METRIC")

    stage = snapshot.targetStage
    requirement = policy.sampleRequirements[stage]
    gates = [
        GateResult(
            "prePromotionDataValidation",
            snapshot.dataValidationPassed,
            snapshot.dataValidationEvidenceSha256,
            "validated-evidence-sha256",
        ),
        GateResult(
            "minimumConsecutiveDays",
            snapshot.consecutiveDays >= requirement.minimumConsecutiveDays,
            snapshot.consecutiveDays,
            requirement.minimumConsecutiveDays,
        ),
        GateResult(
            "minimumCompletedBookings",
            snapshot.completedBookings >= requirement.minimumCompletedBookings,
            snapshot.completedBookings,
            requirement.minimumCompletedBookings,
        ),
    ]
    if requirement.minimumSessionsPerVariant > 0:
        has_two_variants = len(snapshot.sessionsByVariant) == 2
        minimum_observed = min(snapshot.sessionsByVariant.values(), default=0)
        gates.append(
            GateResult(
                "minimumSessionsPerVariant",
                has_two_variants and minimum_observed >= requirement.minimumSessionsPerVariant,
                minimum_observed,
                requirement.minimumSessionsPerVariant,
            )
        )
    if stage == "pilotToRollout":
        gates.extend(
            (
                GateResult("poweredSample", snapshot.poweredSample, snapshot.poweredSample, True),
                GateResult(
                    "confidenceLevel",
                    math.isclose(snapshot.confidenceLevel, policy.confidenceLevel),
                    snapshot.confidenceLevel,
                    policy.confidenceLevel,
                ),
            )
        )

    threshold_attribute = f"{stage}Threshold"
    required_metrics = [
        metric for metric in policy.metrics if getattr(metric, threshold_attribute) is not None
    ]
    missing = {metric.metricKey for metric in required_metrics} - set(snapshot.metricValues)
    if missing:
        raise ValueError("PROMOTION_REQUIRED_METRIC_MISSING")
    for metric in required_metrics:
        observed = snapshot.metricValues[metric.metricKey]
        threshold = getattr(metric, threshold_attribute)
        assert threshold is not None
        passed = observed >= threshold if metric.direction == "minimum" else observed <= threshold
        gates.append(GateResult(metric.metricKey, passed, observed, threshold))

    # El baseline sintético solo compara métricas offline presentes en ambos artefactos. Nunca se usa
    # como sustituto de un control online ni para proclamar incrementalidad.
    metric_by_key = {metric.metricKey: metric for metric in policy.metrics}
    for key, baseline_value in baseline.metrics.items():
        if key not in snapshot.metricValues or key not in metric_by_key:
            raise ValueError("PROMOTION_BASELINE_METRIC_MISSING")
        direction = metric_by_key[key].direction
        observed = snapshot.metricValues[key]
        passed = observed >= baseline_value if direction == "minimum" else observed <= baseline_value
        gates.append(GateResult(f"baseline:{key}", passed, observed, baseline_value))

    return PromotionDecision(
        policyVersion=policy.policyVersion,
        targetStage=stage,
        promotable=all(gate.passed for gate in gates),
        gateResults=tuple(gates),
    )


def _validate_versions(
    policy: PromotionPolicy, baseline: BaselineArtifact, snapshot: PromotionSnapshot
) -> None:
    """Evita evaluar resultados contra dataset, baseline o política distintos."""
    if (
        baseline.baselineVersion != policy.baselineVersion
        or baseline.datasetVersion != policy.datasetVersion
        or snapshot.policyVersion != policy.policyVersion
        or snapshot.datasetVersion != policy.datasetVersion
        or snapshot.baselineVersion != policy.baselineVersion
        or snapshot.dataValidationPolicyVersion != policy.requiredDataValidationPolicyVersion
    ):
        raise ValueError("PROMOTION_VERSION_MISMATCH")


def run() -> None:
    """CLI local: recibe un snapshot JSON y emite la decisión agregada por stdout."""
    import argparse

    parser = argparse.ArgumentParser(description="Evalúa puertas de promoción del ranking MVP")
    parser.add_argument("snapshot", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    policy = PromotionPolicy.load(root / "policies/promotion-gates.v1.json")
    baseline = BaselineArtifact.load(
        root / "evaluation/baselines/public-availability-fallback.v1.json"
    )
    snapshot = PromotionSnapshot.model_validate_json(args.snapshot.read_text(encoding="utf-8"))
    decision = evaluate_promotion(policy, baseline, snapshot)
    print(json.dumps(decision.as_dict(), indent=2, sort_keys=True))
    if not decision.promotable:
        raise SystemExit(1)


if __name__ == "__main__":
    run()
