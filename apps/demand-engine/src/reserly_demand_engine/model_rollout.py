"""Controlador gobernado de shadow/canary, aliases, rollback y fallback a reglas.

La selección de tráfico es estable por requestId técnico, nunca por identidad. Shadow no tiene
autoridad de respuesta; canary avanza por escalones solo con comparación champion completa. La
promoción final exige actor humano. Rollback y degradación a reglas sí son automáticos y fail-closed.
"""

from __future__ import annotations

import hashlib
import threading
from contextlib import contextmanager
from dataclasses import dataclass
from pathlib import Path
from typing import Annotated, Iterator, Literal, Protocol
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


RegistryVersion = Annotated[
    str, Field(pattern=r"^(?:[1-9][0-9]*|[a-z][A-Za-z0-9._-]{0,63})$")
]


class RolloutPolicy(StrictContract):
    """Escalones y guardrails exactos de un rollout."""

    schemaVersion: Literal[1]
    policyVersion: Version
    fallbackPolicyVersion: Version
    minimumShadowRequests: int = Field(ge=1)
    minimumCanaryRequestsPerStep: int = Field(ge=1)
    canaryTrafficBasisPoints: list[int] = Field(min_length=1)
    maximumErrorRate: float = Field(ge=0.0, le=1.0)
    maximumErrorRateDelta: float = Field(ge=0.0, le=1.0)
    maximumLatencyP95Ms: float = Field(gt=0.0)
    maximumLatencyDeltaMs: float = Field(ge=0.0)
    maximumFallbackRate: float = Field(ge=0.0, le=1.0)
    maximumCalibrationError: float = Field(ge=0.0, le=1.0)
    maximumBiasGap: float = Field(ge=0.0, le=1.0)
    maximumDriftPsi: float = Field(ge=0.0)
    minimumQualityDelta: float = Field(ge=-1.0, le=1.0)
    automaticPromotionAllowed: Literal[False]
    automaticRollbackRequired: Literal[True]

    @model_validator(mode="after")
    def validate_steps(self) -> "RolloutPolicy":
        if (
            self.canaryTrafficBasisPoints != sorted(set(self.canaryTrafficBasisPoints))
            or self.canaryTrafficBasisPoints[-1] != 10_000
            or any(value < 1 or value > 10_000 for value in self.canaryTrafficBasisPoints)
        ):
            raise ValueError("ROLLOUT_CANARY_STEPS_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "RolloutPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class RolloutMetrics(StrictContract):
    """Métricas agregadas comparables; no contiene ejemplos, features ni IDs."""

    qualityScore: float = Field(ge=0.0, le=1.0)
    errorRate: float = Field(ge=0.0, le=1.0)
    latencyP95Ms: float = Field(ge=0.0)
    fallbackRate: float = Field(ge=0.0, le=1.0)
    calibrationError: float = Field(ge=0.0, le=1.0)
    biasGap: float = Field(ge=0.0, le=1.0)
    driftPsi: float = Field(ge=0.0)
    privacyViolationCount: int = Field(ge=0)
    hardConstraintViolationCount: int = Field(ge=0)


class RolloutObservation(StrictContract):
    """Ventana de comparación alineada con las dos versiones activas."""

    observationVersion: Literal[1]
    policyVersion: Version
    phase: Literal["shadow", "canary"]
    candidateVersion: RegistryVersion
    championVersion: RegistryVersion
    requests: int = Field(ge=0)
    trafficBasisPoints: int = Field(ge=0, le=10_000)
    candidate: RolloutMetrics
    champion: RolloutMetrics


class DeploymentState(StrictContract):
    """Snapshot CAS del despliegue; revision evita escritores perdidos."""

    stateVersion: Literal[1]
    policyVersion: Version
    modelName: Version
    revision: int = Field(ge=0)
    phase: Literal["shadow", "canary", "champion", "fallback"]
    candidateVersion: RegistryVersion | None
    championVersion: RegistryVersion | None
    previousChampionVersion: RegistryVersion | None
    canaryTrafficBasisPoints: int = Field(ge=0, le=10_000)
    rulesFallbackActive: bool
    killSwitchActive: bool
    lastDecisionCode: Version

    @model_validator(mode="after")
    def validate_state(self) -> "DeploymentState":
        if self.phase in {"shadow", "canary"} and (
            self.candidateVersion is None or self.championVersion is None
        ):
            raise ValueError("ROLLOUT_ACTIVE_VERSIONS_REQUIRED")
        if self.phase == "shadow" and self.canaryTrafficBasisPoints != 0:
            raise ValueError("ROLLOUT_SHADOW_TRAFFIC_FORBIDDEN")
        if self.phase == "fallback" and not self.rulesFallbackActive:
            raise ValueError("ROLLOUT_FALLBACK_STATE_INVALID")
        return self


class PromotionApproval(StrictContract):
    """Aprobación humana ligada a estado, evidencia de datos y decisión de promoción."""

    approvalVersion: Literal[1]
    policyVersion: Version
    modelName: Version
    candidateVersion: RegistryVersion
    expectedChampionVersion: RegistryVersion
    expectedStateRevision: int = Field(ge=0)
    approvedBy: Version
    promotionDecisionSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    dataValidationEvidenceSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    automaticApproval: Literal[False]


class ShadowApproval(StrictContract):
    """Autorización para ejecutar un candidato sin autoridad de respuesta."""

    approvalVersion: Literal[1]
    policyVersion: Version
    modelName: Version
    candidateVersion: RegistryVersion
    expectedChampionVersion: RegistryVersion
    approvedBy: Version
    dataValidationEvidenceSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    automaticApproval: Literal[False]


@dataclass(frozen=True, slots=True)
class RolloutDecision:
    """Transición resultante y motivos opacos aptos para auditoría."""

    state: DeploymentState
    passed: bool
    reviewRequired: bool
    automaticRollback: bool
    failedChecks: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class RouteDecision:
    """Ruta online: versión autoritativa y candidato espejo opcional."""

    mode: Literal["champion", "canary", "shadow", "rulesFallback"]
    modelVersion: str | None
    shadowVersion: str | None
    fallbackPolicyVersion: str | None


def route_request(policy: RolloutPolicy, state: DeploymentState, request_id: UUID) -> RouteDecision:
    """Selecciona ruta determinista; kill switch/estado inválido siempre degradan a reglas."""
    if state.killSwitchActive or state.rulesFallbackActive or state.championVersion is None:
        return RouteDecision("rulesFallback", None, None, policy.fallbackPolicyVersion)
    if state.phase == "shadow":
        return RouteDecision("shadow", state.championVersion, state.candidateVersion, None)
    if state.phase == "canary" and state.candidateVersion is not None:
        bucket = int.from_bytes(
            hashlib.sha256(f"{request_id}:{state.policyVersion}".encode()).digest()[:8], "big"
        ) % 10_000
        if bucket < state.canaryTrafficBasisPoints:
            return RouteDecision("canary", state.candidateVersion, None, None)
    return RouteDecision("champion", state.championVersion, None, None)


def evaluate_rollout(
    policy: RolloutPolicy, state: DeploymentState, observation: RolloutObservation
) -> RolloutDecision:
    """Compara candidato/champion y avanza un escalón o hace rollback automático."""
    if (
        state.policyVersion != policy.policyVersion
        or observation.policyVersion != policy.policyVersion
        or observation.phase != state.phase
        or observation.candidateVersion != state.candidateVersion
        or observation.championVersion != state.championVersion
        or observation.trafficBasisPoints != state.canaryTrafficBasisPoints
    ):
        raise ValueError("ROLLOUT_OBSERVATION_VERSION_MISMATCH")
    required_requests = (
        policy.minimumShadowRequests
        if state.phase == "shadow"
        else policy.minimumCanaryRequestsPerStep
    )
    candidate, champion = observation.candidate, observation.champion
    checks = {
        "minimumRequests": observation.requests >= required_requests,
        "privacy": candidate.privacyViolationCount == 0,
        "hardConstraints": candidate.hardConstraintViolationCount == 0,
        "quality": candidate.qualityScore - champion.qualityScore >= policy.minimumQualityDelta,
        "errorRate": candidate.errorRate <= policy.maximumErrorRate
        and candidate.errorRate - champion.errorRate <= policy.maximumErrorRateDelta,
        "latency": candidate.latencyP95Ms <= policy.maximumLatencyP95Ms
        and candidate.latencyP95Ms - champion.latencyP95Ms <= policy.maximumLatencyDeltaMs,
        "fallbackRate": candidate.fallbackRate <= policy.maximumFallbackRate,
        "calibration": candidate.calibrationError <= policy.maximumCalibrationError,
        "bias": candidate.biasGap <= policy.maximumBiasGap,
        "drift": candidate.driftPsi <= policy.maximumDriftPsi,
    }
    failed = tuple(name for name, passed in checks.items() if not passed)
    if failed:
        rollback = state.model_copy(update={
            "revision": state.revision + 1,
            "phase": "champion",
            "candidateVersion": None,
            "canaryTrafficBasisPoints": 0,
            "rulesFallbackActive": False,
            "lastDecisionCode": "automatic-rollback",
        })
        return RolloutDecision(rollback, False, False, True, failed)

    if state.phase == "shadow":
        next_state = state.model_copy(update={
            "revision": state.revision + 1,
            "phase": "canary",
            "canaryTrafficBasisPoints": policy.canaryTrafficBasisPoints[0],
            "lastDecisionCode": "shadow-passed",
        })
        return RolloutDecision(next_state, True, False, False, ())

    current_index = policy.canaryTrafficBasisPoints.index(state.canaryTrafficBasisPoints)
    if current_index < len(policy.canaryTrafficBasisPoints) - 1:
        next_state = state.model_copy(update={
            "revision": state.revision + 1,
            "canaryTrafficBasisPoints": policy.canaryTrafficBasisPoints[current_index + 1],
            "lastDecisionCode": "canary-step-passed",
        })
        return RolloutDecision(next_state, True, False, False, ())
    awaiting_review = state.model_copy(update={
        "revision": state.revision + 1,
        "lastDecisionCode": "promotion-review-required",
    })
    return RolloutDecision(awaiting_review, True, True, False, ())


class AliasClient(Protocol):
    """Superficie de registry utilizada bajo lock distribuido obligatorio."""

    def get_alias(self, model_name: str, alias: str) -> str | None: ...
    def set_alias(self, model_name: str, alias: str, version: str) -> None: ...


class LockProvider(Protocol):
    """Lock exclusivo compartido por todos los registradores de un modelo."""

    def hold(self, key: str) -> Iterator[None]: ...


class InMemoryAliasClient:
    """Registry determinista para pruebas; puede inyectar fallo en el siguiente write."""

    def __init__(self, champion: str | None = None) -> None:
        self.aliases = {"champion": champion} if champion else {}
        self.fail_alias_once: str | None = None

    def get_alias(self, model_name: str, alias: str) -> str | None:
        return self.aliases.get(alias)

    def set_alias(self, model_name: str, alias: str, version: str) -> None:
        if self.fail_alias_once == alias:
            self.fail_alias_once = None
            raise RuntimeError("REGISTRY_WRITE_FAILED")
        self.aliases[alias] = version


class MlflowAliasClient:
    """Adaptador de aliases MLflow; la identidad registration se inyecta fuera del código."""

    def __init__(self, tracking_uri: str) -> None:
        from mlflow import MlflowClient

        self._client = MlflowClient(tracking_uri)

    def get_alias(self, model_name: str, alias: str) -> str | None:
        try:
            return str(self._client.get_model_version_by_alias(model_name, alias).version)
        except Exception as error:
            if "RESOURCE_DOES_NOT_EXIST" in str(error) or "not found" in str(error).lower():
                return None
            raise

    def set_alias(self, model_name: str, alias: str, version: str) -> None:
        self._client.set_registered_model_alias(model_name, alias, version)


class InMemoryLockProvider:
    """Lock de proceso para pruebas; producción debe usar PostgreSQL/Redis con lease."""

    def __init__(self) -> None:
        self._lock = threading.Lock()

    @contextmanager
    def hold(self, key: str) -> Iterator[None]:
        with self._lock:
            yield


def begin_shadow(
    policy: RolloutPolicy,
    approval: ShadowApproval,
    registry: AliasClient,
    locks: LockProvider,
) -> DeploymentState:
    """Publica alias shadow bajo CAS; el champion sigue siendo la única respuesta online."""
    if approval.policyVersion != policy.policyVersion or (
        approval.candidateVersion == approval.expectedChampionVersion
    ):
        raise ValueError("ROLLOUT_SHADOW_APPROVAL_INVALID")
    with locks.hold(f"mlflow-alias:{approval.modelName}"):
        current = registry.get_alias(approval.modelName, "champion")
        if current != approval.expectedChampionVersion:
            raise ValueError("ROLLOUT_CHAMPION_COMPARE_AND_SWAP_FAILED")
        registry.set_alias(approval.modelName, "shadow", approval.candidateVersion)
    return DeploymentState(
        stateVersion=1,
        policyVersion=policy.policyVersion,
        modelName=approval.modelName,
        revision=1,
        phase="shadow",
        candidateVersion=approval.candidateVersion,
        championVersion=current,
        previousChampionVersion=None,
        canaryTrafficBasisPoints=0,
        rulesFallbackActive=False,
        killSwitchActive=False,
        lastDecisionCode="human-approved-shadow",
    )


def promote_champion(
    policy: RolloutPolicy,
    state: DeploymentState,
    approval: PromotionApproval,
    registry: AliasClient,
    locks: LockProvider,
) -> DeploymentState:
    """Cambia aliases bajo lock, conserva champion previo y compensa cualquier fallo parcial."""
    if (
        policy.automaticPromotionAllowed
        or state.phase != "canary"
        or state.canaryTrafficBasisPoints != 10_000
        or state.lastDecisionCode != "promotion-review-required"
        or approval.policyVersion != policy.policyVersion
        or approval.modelName != state.modelName
        or approval.candidateVersion != state.candidateVersion
        or approval.expectedChampionVersion != state.championVersion
        or approval.expectedStateRevision != state.revision
    ):
        raise ValueError("ROLLOUT_PROMOTION_APPROVAL_INVALID")
    assert state.candidateVersion and state.championVersion
    with locks.hold(f"mlflow-alias:{state.modelName}"):
        current = registry.get_alias(state.modelName, "champion")
        if current != state.championVersion:
            raise ValueError("ROLLOUT_CHAMPION_COMPARE_AND_SWAP_FAILED")
        registry.set_alias(state.modelName, "previous-champion", current)
        try:
            registry.set_alias(state.modelName, "champion", state.candidateVersion)
        except Exception:
            # El primer alias no cambia servicio; restaurar champion hace explícita la compensación.
            registry.set_alias(state.modelName, "champion", current)
            raise
    return state.model_copy(update={
        "revision": state.revision + 1,
        "phase": "champion",
        "previousChampionVersion": state.championVersion,
        "championVersion": state.candidateVersion,
        "candidateVersion": None,
        "canaryTrafficBasisPoints": 0,
        "lastDecisionCode": "human-approved-promotion",
    })


def activate_rules_fallback(state: DeploymentState, reason: Version) -> DeploymentState:
    """Degrada sin modelo cuando registry/rollback no son fiables; no inventa una versión."""
    return state.model_copy(update={
        "revision": state.revision + 1,
        "phase": "fallback",
        "candidateVersion": None,
        "canaryTrafficBasisPoints": 0,
        "rulesFallbackActive": True,
        "lastDecisionCode": reason,
    })


def execute_automatic_rollback(
    state: DeploymentState, registry: AliasClient, locks: LockProvider
) -> DeploymentState:
    """Restaura champion bajo lock; si registry no es fiable activa reglas sin propagar modelo."""
    if state.lastDecisionCode != "automatic-rollback" or state.championVersion is None:
        raise ValueError("ROLLOUT_AUTOMATIC_ROLLBACK_INVALID")
    try:
        with locks.hold(f"mlflow-alias:{state.modelName}"):
            if registry.get_alias(state.modelName, "champion") != state.championVersion:
                registry.set_alias(state.modelName, "champion", state.championVersion)
    except Exception:
        return activate_rules_fallback(state, "rollback-registry-failed")
    return state
