"""Aprendizaje River prequential en sombra con drift y rollback fail-closed."""

from __future__ import annotations

from collections import Counter, defaultdict
from datetime import datetime
import hashlib
import json
import math
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator
import river
from river import drift, linear_model, optim, preprocessing, utils

from .contracts import RequestEnvelope, StrictContract, Version


class IncrementalLearningPolicy(StrictContract):
    """Versiona features, detectores, gates y fallback del challenger online en sombra."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelVersion: Version
    featureSetVersion: Version
    riverVersion: Version
    featureNames: list[Version] = Field(min_length=1, max_length=32)
    learningRate: float = Field(gt=0, le=1)
    maximumAbsoluteFeatureValue: float = Field(gt=0, le=1_000_000)
    minimumReferenceSize: int = Field(ge=32, le=100_000)
    minimumUpdateBatchSize: int = Field(ge=1, le=100_000)
    adwinDelta: float = Field(gt=0, lt=1)
    adwinClock: int = Field(ge=1, le=1_000)
    adwinGracePeriod: int = Field(ge=1, le=100_000)
    pageHinkleyMinimumInstances: int = Field(ge=1, le=100_000)
    pageHinkleyDelta: float = Field(ge=0, le=1)
    pageHinkleyThreshold: float = Field(gt=0)
    pageHinkleyAlpha: float = Field(gt=0, le=1)
    maximumMeanAbsoluteErrorIncrease: float = Field(ge=0, le=1)
    fallbackPolicyVersion: Version
    automaticPromotionAllowed: Literal[False]
    onlineDeploymentAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "IncrementalLearningPolicy":
        if len(self.featureNames) != len(set(self.featureNames)):
            raise ValueError("INCREMENTAL_FEATURES_DUPLICATED")
        return self

    @classmethod
    def load(cls, path: Path) -> "IncrementalLearningPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class IncrementalModelCard(StrictContract):
    """Registra finalidad, límites, dependencia y rollback del challenger incremental."""

    schemaVersion: Literal[1]
    modelKey: Version
    modelVersion: Version
    owner: Version
    purpose: str = Field(min_length=20, max_length=500)
    status: Literal["shadow"]
    libraryVersion: Version
    featureSetVersion: Version
    intendedUse: list[Version] = Field(min_length=1)
    prohibitedUse: list[Version] = Field(min_length=1)
    limitations: list[str] = Field(min_length=1)
    rollback: str = Field(min_length=20, max_length=500)
    humanApprovalRequired: Literal[True]
    automaticPromotionAllowed: Literal[False]

    @classmethod
    def load(cls, path: Path) -> "IncrementalModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class IncrementalModelState(StrictContract):
    """Checkpoint JSON reconstruible; evita serialización pickle de objetos ejecutables."""

    modelVersion: Version
    featureSetVersion: Version
    lastSequence: int = Field(ge=0)
    trainingCount: int = Field(ge=0)
    featureCounts: dict[Version, int]
    featureMeans: dict[Version, float]
    featureVariances: dict[Version, float]
    weights: dict[Version, float]
    intercept: float
    optimizerIterations: int = Field(ge=0)
    stateChecksum: str = Field(pattern=r"^[0-9a-f]{64}$")

    @model_validator(mode="after")
    def finite_values(self) -> "IncrementalModelState":
        values = [
            *self.featureMeans.values(), *self.featureVariances.values(),
            *self.weights.values(), self.intercept,
        ]
        if not all(math.isfinite(value) for value in values):
            raise ValueError("INCREMENTAL_STATE_NON_FINITE")
        if any(value < 0 for value in self.featureVariances.values()):
            raise ValueError("INCREMENTAL_STATE_VARIANCE_INVALID")
        return self


class IncrementalObservation(StrictContract):
    """Outcome maduro, minimizado y ordenado; no contiene sujeto ni datos sensibles."""

    observationId: UUID
    sequence: int = Field(ge=1)
    occurredAt: datetime
    outcomeObservedAt: datetime
    features: dict[Version, float]
    completedBooking: Literal[0, 1]

    @model_validator(mode="after")
    def validate_observation(self) -> "IncrementalObservation":
        if (
            self.occurredAt.tzinfo is None or self.outcomeObservedAt.tzinfo is None
            or self.outcomeObservedAt < self.occurredAt
            or not all(math.isfinite(value) for value in self.features.values())
        ):
            raise ValueError("INCREMENTAL_OBSERVATION_INVALID")
        return self


class IncrementalLearningRequest(RequestEnvelope):
    """Microbatch shadow con referencia fija; Spring persiste checkpoints mediante CAS."""

    modelVersion: Version
    featureSetVersion: Version
    productionEvidence: bool
    purpose: Literal["shadowIncrementalEvaluation"]
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    priorState: IncrementalModelState
    referenceAbsoluteErrors: list[float] = Field(min_length=32, max_length=100_000)
    referenceFeatureValues: dict[Version, list[float]]
    observations: list[IncrementalObservation] = Field(min_length=1, max_length=10_000)

    @model_validator(mode="after")
    def validate_batch(self) -> "IncrementalLearningRequest":
        ids = [item.observationId for item in self.observations]
        if (
            len(ids) != len(set(ids))
            or any(not math.isfinite(value) or value < 0 or value > 1
                   for value in self.referenceAbsoluteErrors)
            or any(not values or any(not math.isfinite(value) for value in values)
                   for values in self.referenceFeatureValues.values())
            or any(item.outcomeObservedAt > self.occurredAt for item in self.observations)
        ):
            raise ValueError("INCREMENTAL_BATCH_INVALID")
        return self


class DriftSignal(StrictContract):
    """Alarma explicable por métrica y detector, sin incluir observaciones individuales."""

    metric: Version
    detector: Literal["ADWIN", "PageHinkley", "meanErrorGuardrail"]
    detected: bool
    referenceMean: float
    observedMean: float


class IncrementalLearningResponse(StrictContract):
    """Resultado de sombra: un checkpoint candidato jamás equivale a despliegue online."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelVersion: Version
    featureSetVersion: Version
    riverVersion: Version
    status: Literal["candidateUpdated", "driftBlocked", "insufficientBatch"]
    processedCount: int = Field(ge=1)
    referenceMeanAbsoluteError: float = Field(ge=0, le=1)
    prequentialMeanAbsoluteError: float = Field(ge=0, le=1)
    driftDetected: bool
    driftSignals: list[DriftSignal]
    candidateState: IncrementalModelState | None
    humanReviewAllowed: bool
    rollbackRequired: bool
    fallbackPolicyVersion: Version
    automaticPromotionAllowed: Literal[False]
    onlineDeploymentAllowed: Literal[False]


class IncrementalLearningMonitor:
    """Reconstruye River, predice-antes-de-aprender y descarta updates contaminados por drift."""

    def __init__(self, policy: IncrementalLearningPolicy, model_card: IncrementalModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            f"river-{river.__version__}" != policy.riverVersion
            or model_card.libraryVersion != policy.riverVersion
            or model_card.modelVersion != policy.modelVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("INCREMENTAL_RIVER_VERSION_MISMATCH")

    def empty_state(self) -> IncrementalModelState:
        """Crea el único checkpoint inicial válido para bootstrap controlado."""
        payload = {
            "modelVersion": self.policy.modelVersion,
            "featureSetVersion": self.policy.featureSetVersion,
            "lastSequence": 0, "trainingCount": 0,
            "featureCounts": {}, "featureMeans": {}, "featureVariances": {},
            "weights": {}, "intercept": 0.0, "optimizerIterations": 0,
        }
        return IncrementalModelState(**payload, stateChecksum=self._checksum(payload))

    def evaluate(self, request: IncrementalLearningRequest) -> IncrementalLearningResponse:
        """Evalúa un microbatch de forma reproducible sin mutar el campeón ni una fuente externa."""
        self._validate_request(request)
        pipeline = self._restore(request.priorState)
        errors: list[float] = []
        for observation in request.observations:
            probability = pipeline.predict_proba_one(observation.features).get(True, 0.0)
            errors.append(abs(observation.completedBooking - probability))
            pipeline.learn_one(observation.features, bool(observation.completedBooking))
        signals = self._detect_drift(request, errors)
        detected = any(signal.detected for signal in signals)
        enough = len(request.observations) >= self.policy.minimumUpdateBatchSize
        candidate_state = None if detected or not enough else self._snapshot(
            pipeline, request.priorState, request.observations[-1].sequence
        )
        status = "driftBlocked" if detected else (
            "candidateUpdated" if enough else "insufficientBatch"
        )
        return IncrementalLearningResponse(
            requestId=request.requestId, policyVersion=self.policy.policyVersion,
            modelVersion=self.policy.modelVersion, featureSetVersion=self.policy.featureSetVersion,
            riverVersion=self.policy.riverVersion, status=status,
            processedCount=len(request.observations),
            referenceMeanAbsoluteError=self._mean(request.referenceAbsoluteErrors),
            prequentialMeanAbsoluteError=self._mean(errors), driftDetected=detected,
            driftSignals=signals, candidateState=candidate_state,
            humanReviewAllowed=(enough and request.productionEvidence and not detected),
            rollbackRequired=detected, fallbackPolicyVersion=self.policy.fallbackPolicyVersion,
            automaticPromotionAllowed=False, onlineDeploymentAllowed=False,
        )

    def _validate_request(self, request: IncrementalLearningRequest) -> None:
        state = request.priorState
        state_payload = state.model_dump(exclude={"stateChecksum"})
        sequences = [item.sequence for item in request.observations]
        expected = list(range(state.lastSequence + 1, state.lastSequence + 1 + len(sequences)))
        feature_names = set(self.policy.featureNames)
        if (
            request.policyVersion != self.policy.policyVersion
            or request.modelVersion != self.policy.modelVersion
            or request.featureSetVersion != self.policy.featureSetVersion
            or state.modelVersion != self.policy.modelVersion
            or state.featureSetVersion != self.policy.featureSetVersion
            or state.stateChecksum != self._checksum(state_payload)
            or sequences != expected
            or set(request.referenceFeatureValues) != feature_names
            or any(len(values) < self.policy.minimumReferenceSize
                   for values in request.referenceFeatureValues.values())
            or len(request.referenceAbsoluteErrors) < self.policy.minimumReferenceSize
            or any(set(item.features) != feature_names for item in request.observations)
            or any(abs(value) > self.policy.maximumAbsoluteFeatureValue
                   for item in request.observations for value in item.features.values())
            or any(abs(value) > self.policy.maximumAbsoluteFeatureValue
                   for values in request.referenceFeatureValues.values() for value in values)
            or any(set(mapping) - feature_names for mapping in (
                state.featureCounts, state.featureMeans, state.featureVariances, state.weights
            ))
        ):
            raise ValueError("INCREMENTAL_REQUEST_REJECTED")

    def _restore(self, state: IncrementalModelState):
        pipeline = preprocessing.StandardScaler() | linear_model.LogisticRegression(
            optimizer=optim.SGD(self.policy.learningRate),
            intercept_lr=self.policy.learningRate,
        )
        scaler = pipeline.steps["StandardScaler"]
        learner = pipeline.steps["LogisticRegression"]
        scaler.counts = Counter(state.featureCounts)
        scaler.means = defaultdict(float, state.featureMeans)
        scaler.vars = defaultdict(float, state.featureVariances)
        # River puede adoptar el mapping sin copiarlo; copy=True protege el checkpoint inmutable.
        learner._weights = utils.VectorDict(state.weights, copy=True)
        learner.intercept = state.intercept
        learner.optimizer.n_iterations = state.optimizerIterations
        return pipeline

    def _snapshot(self, pipeline, prior: IncrementalModelState, last_sequence: int):
        scaler = pipeline.steps["StandardScaler"]
        learner = pipeline.steps["LogisticRegression"]
        payload = {
            "modelVersion": self.policy.modelVersion,
            "featureSetVersion": self.policy.featureSetVersion,
            "lastSequence": last_sequence,
            "trainingCount": prior.trainingCount + (last_sequence - prior.lastSequence),
            "featureCounts": dict(sorted(scaler.counts.items())),
            "featureMeans": dict(sorted(scaler.means.items())),
            "featureVariances": dict(sorted(scaler.vars.items())),
            "weights": dict(sorted(learner.weights.items())),
            "intercept": learner.intercept,
            "optimizerIterations": learner.optimizer.n_iterations,
        }
        return IncrementalModelState(**payload, stateChecksum=self._checksum(payload))

    def _detect_drift(self, request, errors):
        error_detector = drift.ADWIN(
            delta=self.policy.adwinDelta, clock=self.policy.adwinClock,
            grace_period=self.policy.adwinGracePeriod,
        )
        for value in request.referenceAbsoluteErrors:
            error_detector.update(value)
        error_drift = False
        for value in errors:
            error_detector.update(value)
            error_drift = error_drift or error_detector.drift_detected
        reference_error = self._mean(request.referenceAbsoluteErrors)
        observed_error = self._mean(errors)
        signals = [
            DriftSignal(metric="absoluteError", detector="ADWIN", detected=error_drift,
                        referenceMean=reference_error, observedMean=observed_error),
            DriftSignal(
                metric="absoluteErrorIncrease", detector="meanErrorGuardrail",
                detected=observed_error - reference_error > self.policy.maximumMeanAbsoluteErrorIncrease,
                referenceMean=reference_error, observedMean=observed_error,
            ),
        ]
        for feature in self.policy.featureNames:
            detector = drift.PageHinkley(
                min_instances=self.policy.pageHinkleyMinimumInstances,
                delta=self.policy.pageHinkleyDelta,
                threshold=self.policy.pageHinkleyThreshold,
                alpha=self.policy.pageHinkleyAlpha, mode="both",
            )
            reference = request.referenceFeatureValues[feature]
            for value in reference:
                detector.update(value)
            feature_drift = False
            observed = [item.features[feature] for item in request.observations]
            for value in observed:
                detector.update(value)
                feature_drift = feature_drift or detector.drift_detected
            signals.append(DriftSignal(
                metric=feature, detector="PageHinkley", detected=feature_drift,
                referenceMean=self._mean(reference), observedMean=self._mean(observed),
            ))
        return signals

    @staticmethod
    def _mean(values: list[float]) -> float:
        return round(sum(values) / len(values), 8)

    @staticmethod
    def _checksum(payload: dict[str, object]) -> str:
        encoded = json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=True)
        return hashlib.sha256(encoded.encode("utf-8")).hexdigest()
