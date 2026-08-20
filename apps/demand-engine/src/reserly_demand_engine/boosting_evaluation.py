"""Comparación real CatBoost vs logística con calibración, latencia, estabilidad y equidad."""

from __future__ import annotations

import math
import time
from pathlib import Path
from typing import Literal

import catboost
import numpy as np
from catboost import CatBoostClassifier
from pydantic import Field, model_validator

from .contracts import StrictContract, Version
from .conversion_training import (
    ConversionDataset,
    ConversionLogisticTrainer,
    ConversionMetrics,
    ConversionModelCard,
    ConversionTrainingPolicy,
    _fit_logistic,
    _metrics,
    _sigmoid,
)


class BoostingComparisonPolicy(StrictContract):
    """Gates incrementales obligatorios del challenger frente al baseline."""

    schemaVersion: Literal[1]
    policyVersion: Version
    candidateLibrary: Literal["catboost"]
    candidateLibraryVersion: str
    candidateLicense: Literal["Apache-2.0"]
    baselineModelVersion: Version
    minimumRocAucGain: float = Field(ge=0, le=1)
    maximumBrierRegression: float = Field(ge=0, le=1)
    maximumEceRegression: float = Field(ge=0, le=1)
    maximumLatencyP95Ms: float = Field(gt=0)
    maximumStabilityDelta: float = Field(ge=0, le=1)
    minimumRowsPerAuditSegment: int = Field(ge=5)
    maximumSegmentBrierGap: float = Field(ge=0, le=1)
    maximumArtifactBytes: int = Field(ge=1)
    predictionRepeats: int = Field(ge=5, le=1000)

    @classmethod
    def load(cls, path: Path) -> "BoostingComparisonPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class BoostingModelCard(StrictContract):
    """Procedencia, licencia, limitaciones, revisión de seguridad y rollback."""

    schemaVersion: Literal[1]
    modelKey: Version
    modelVersion: Version
    owner: Version
    purpose: str = Field(min_length=20)
    status: Literal["candidate"]
    library: Literal["catboost"]
    libraryVersion: str
    license: Literal["Apache-2.0"]
    source: str
    cveReviewRequiredBeforePromotion: Literal[True]
    limitations: list[str] = Field(min_length=1)
    rollback: str = Field(min_length=20)
    humanApprovalRequired: Literal[True]

    @classmethod
    def load(cls, path: Path) -> "BoostingModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class AuditSegmentMetric(StrictContract):
    """Brier agregado de una cohorte no sensible que nunca entra como feature."""

    segment: Literal["es", "en"]
    rows: int
    brier: float


class BoostingComparisonResult(StrictContract):
    """Decisión completa de challenger; distingue superar gates de autorizar promoción."""

    policyVersion: Version
    baselineModelVersion: Version
    candidateModelVersion: Version
    datasetVersion: Version
    libraryVersion: str
    baselineMetrics: ConversionMetrics
    candidateMetrics: ConversionMetrics
    rocAucGain: float
    brierRegression: float
    eceRegression: float
    latencyP95Ms: float
    stabilityMaximumDelta: float
    artifactBytes: int
    auditSegments: list[AuditSegmentMetric]
    maximumSegmentBrierGap: float
    qualityGatesPassed: bool
    cveReviewApproved: bool
    productionEvidence: bool
    promotionAllowed: bool
    blockingReasons: list[Version]


class CatBoostConversionEvaluator:
    """Entrena challenger real en train, calibra en calibration y compara solo en evaluation."""

    def __init__(
        self,
        comparison_policy: BoostingComparisonPolicy,
        training_policy: ConversionTrainingPolicy,
        baseline_card: ConversionModelCard,
        candidate_card: BoostingModelCard,
    ) -> None:
        self.policy = comparison_policy
        self.training_policy = training_policy
        self.baseline_card = baseline_card
        self.candidate_card = candidate_card
        if (
            catboost.__version__ != comparison_policy.candidateLibraryVersion
            or candidate_card.libraryVersion != comparison_policy.candidateLibraryVersion
            or candidate_card.license != comparison_policy.candidateLicense
            or comparison_policy.baselineModelVersion != baseline_card.modelVersion
        ):
            raise ValueError("BOOSTING_DEPENDENCY_OR_MODEL_VERSION_MISMATCH")

    def evaluate(
        self, dataset: ConversionDataset, *, cve_review_approved: bool = False
    ) -> BoostingComparisonResult:
        """Compara en la misma cohorte; nunca promociona solo por calidad sintética."""
        baseline = ConversionLogisticTrainer(self.training_policy, self.baseline_card).train(dataset)
        train, calibration, evaluation = self._split(dataset)
        feature_codes = self.training_policy.featureCodes
        x_train = np.asarray([[row.features[code] for code in feature_codes] for row in train])
        y_train = np.asarray([row.converted for row in train])
        x_calibration = np.asarray(
            [[row.features[code] for code in feature_codes] for row in calibration]
        )
        x_evaluation = np.asarray(
            [[row.features[code] for code in feature_codes] for row in evaluation]
        )
        model = self._fit_model(x_train, y_train)
        candidate_probabilities = self._calibrated_probabilities(
            model, x_calibration, [row.converted for row in calibration], x_evaluation
        )
        candidate_metrics = _metrics(
            [row.converted for row in evaluation], candidate_probabilities
        )
        repeated_model = self._fit_model(x_train, y_train)
        repeated_probabilities = self._calibrated_probabilities(
            repeated_model,
            x_calibration,
            [row.converted for row in calibration],
            x_evaluation,
        )
        stability = max(
            abs(first - second)
            for first, second in zip(candidate_probabilities, repeated_probabilities, strict=True)
        )
        latency = self._latency_p95(model, x_evaluation)
        # La serialización nativa permite medir el artefacto real sin escribir datos al disco.
        artifact_bytes = len(model._serialize_model())
        segments = self._segments(evaluation, candidate_probabilities)
        segment_gap = max(item.brier for item in segments) - min(item.brier for item in segments)
        auc_gain = candidate_metrics.rocAuc - baseline.evaluationMetrics.rocAuc
        brier_regression = candidate_metrics.brier - baseline.evaluationMetrics.brier
        ece_regression = (
            candidate_metrics.expectedCalibrationError
            - baseline.evaluationMetrics.expectedCalibrationError
        )
        quality = (
            auc_gain >= self.policy.minimumRocAucGain
            and brier_regression <= self.policy.maximumBrierRegression
            and ece_regression <= self.policy.maximumEceRegression
            and latency <= self.policy.maximumLatencyP95Ms
            and stability <= self.policy.maximumStabilityDelta
            and segment_gap <= self.policy.maximumSegmentBrierGap
            and artifact_bytes <= self.policy.maximumArtifactBytes
        )
        reasons: list[str] = []
        if not quality:
            reasons.append("qualityGateFailed")
        if not dataset.productionEvidence:
            reasons.append("productionEvidenceMissing")
        if not cve_review_approved:
            reasons.append("cveReviewMissing")
        return BoostingComparisonResult(
            policyVersion=self.policy.policyVersion,
            baselineModelVersion=baseline.modelVersion,
            candidateModelVersion=self.candidate_card.modelVersion,
            datasetVersion=dataset.datasetVersion,
            libraryVersion=catboost.__version__,
            baselineMetrics=baseline.evaluationMetrics,
            candidateMetrics=candidate_metrics,
            rocAucGain=round(auc_gain, 8),
            brierRegression=round(brier_regression, 8),
            eceRegression=round(ece_regression, 8),
            latencyP95Ms=round(latency, 8),
            stabilityMaximumDelta=round(stability, 12),
            artifactBytes=artifact_bytes,
            auditSegments=segments,
            maximumSegmentBrierGap=round(segment_gap, 8),
            qualityGatesPassed=quality,
            cveReviewApproved=cve_review_approved,
            productionEvidence=dataset.productionEvidence,
            promotionAllowed=quality and dataset.productionEvidence and cve_review_approved,
            blockingReasons=reasons,
        )

    def _split(self, dataset: ConversionDataset):
        train = [row for row in dataset.rows if row.occurredAt < self.training_policy.trainingEndsBefore]
        calibration = [
            row
            for row in dataset.rows
            if self.training_policy.trainingEndsBefore
            <= row.occurredAt
            < self.training_policy.calibrationEndsBefore
        ]
        evaluation = [
            row
            for row in dataset.rows
            if self.training_policy.calibrationEndsBefore
            <= row.occurredAt
            < self.training_policy.evaluationEndsBefore
        ]
        return train, calibration, evaluation

    def _fit_model(self, features: np.ndarray, labels: np.ndarray) -> CatBoostClassifier:
        model = CatBoostClassifier(
            loss_function="Logloss",
            iterations=60,
            learning_rate=0.05,
            depth=3,
            l2_leaf_reg=0.5,
            random_seed=17,
            random_strength=0,
            thread_count=1,
            allow_writing_files=False,
            verbose=False,
        )
        model.fit(features, labels)
        return model

    def _calibrated_probabilities(
        self,
        model: CatBoostClassifier,
        calibration_features: np.ndarray,
        calibration_labels: list[int],
        evaluation_features: np.ndarray,
    ) -> list[float]:
        calibration_logits = model.predict(
            calibration_features, prediction_type="RawFormulaVal"
        ).tolist()
        intercept, weights = _fit_logistic(
            [[value] for value in calibration_logits],
            calibration_labels,
            self.training_policy.calibrationEpochs,
            self.training_policy.learningRate,
            self.training_policy.l2Penalty,
        )
        evaluation_logits = model.predict(
            evaluation_features, prediction_type="RawFormulaVal"
        ).tolist()
        return [_sigmoid(intercept + weights[0] * value) for value in evaluation_logits]

    def _latency_p95(self, model: CatBoostClassifier, features: np.ndarray) -> float:
        model.predict_proba(features)
        timings = []
        for _ in range(self.policy.predictionRepeats):
            started = time.perf_counter_ns()
            model.predict_proba(features)
            timings.append((time.perf_counter_ns() - started) / 1_000_000)
        timings.sort()
        return timings[math.ceil(0.95 * len(timings)) - 1]

    def _segments(self, rows, probabilities: list[float]) -> list[AuditSegmentMetric]:
        metrics = []
        for segment in ("es", "en"):
            indexes = [index for index, row in enumerate(rows) if row.evaluationSegment == segment]
            if len(indexes) < self.policy.minimumRowsPerAuditSegment:
                raise ValueError("BOOSTING_AUDIT_SEGMENT_SAMPLE_INSUFFICIENT")
            brier = sum(
                (probabilities[index] - rows[index].converted) ** 2 for index in indexes
            ) / len(indexes)
            metrics.append(AuditSegmentMetric(segment=segment, rows=len(indexes), brier=round(brier, 8)))
        return metrics
