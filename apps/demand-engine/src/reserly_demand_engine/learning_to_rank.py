"""Evaluación offline gobernada de LambdaMART frente al ranking baseline."""

from __future__ import annotations

import hashlib
import math
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

import numpy as np
from pydantic import Field, model_validator
from xgboost import XGBRanker

from .contracts import StrictContract, Version


class LearningToRankPolicy(StrictContract):
    """Fija splits, algoritmo, métricas incrementales y fronteras de features."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    trainingEndsBefore: datetime
    evaluationEndsBefore: datetime
    minimumQueriesPerSplit: int = Field(ge=5)
    minimumCandidatesPerQuery: int = Field(ge=2)
    topK: int = Field(ge=1, le=20)
    nEstimators: int = Field(ge=10, le=2_000)
    maximumDepth: int = Field(ge=1, le=16)
    learningRate: float = Field(gt=0, le=1)
    l2Penalty: float = Field(ge=0, le=100)
    randomSeed: int
    minimumNdcgGain: float = Field(ge=0, le=1)
    minimumConversionGain: float = Field(ge=0, le=1)
    maximumDiversityRegression: float = Field(ge=0, le=1)
    maximumNewVenueExposureRegression: float = Field(ge=0, le=1)
    maximumStabilityDelta: float = Field(ge=0, le=1)
    prohibitedFeatureFragments: list[str] = Field(min_length=1)
    automaticDeploymentAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "LearningToRankPolicy":
        if (
            self.trainingEndsBefore.tzinfo is None
            or self.evaluationEndsBefore.tzinfo is None
            or self.trainingEndsBefore >= self.evaluationEndsBefore
            or self.topK > self.minimumCandidatesPerQuery
        ):
            raise ValueError("LTR_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "LearningToRankPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class LearningToRankModelCard(StrictContract):
    """Describe autoridad, limitaciones, aprobación y rollback del ranker candidato."""

    schemaVersion: Literal[1]
    modelKey: Version
    modelVersion: Version
    owner: Version
    purpose: str = Field(min_length=20)
    intendedUse: list[Version] = Field(min_length=1)
    prohibitedUse: list[Version] = Field(min_length=1)
    status: Literal["candidate"]
    trainingPolicyVersion: Version
    featureSetVersion: Version
    limitations: list[str] = Field(min_length=1)
    rollback: str = Field(min_length=20)
    humanApprovalRequired: Literal[True]

    @classmethod
    def load(cls, path: Path) -> "LearningToRankModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class RankingCandidate(StrictContract):
    """Alternativa ya elegible con features pre-outcome y etiquetas solo para evaluación."""

    candidateId: UUID
    featureValues: list[float] = Field(min_length=1, max_length=128)
    baselineScore: float
    relevance: int = Field(ge=0, le=3)
    converted: Literal[0, 1]
    categoryCode: Version
    isNewVenue: bool
    eligible: Literal[True]
    capacityAvailable: Literal[True]

    @model_validator(mode="after")
    def validate_values(self) -> "RankingCandidate":
        if not math.isfinite(self.baselineScore) or not all(
            math.isfinite(value) for value in self.featureValues
        ):
            raise ValueError("LTR_FEATURE_NON_FINITE")
        return self


class RankingQuery(StrictContract):
    """Conjunto completo de candidatos de una consulta y outcome ya maduro."""

    queryId: UUID
    occurredAt: datetime
    outcomeObservedAt: datetime
    completeCandidateSet: Literal[True]
    candidates: list[RankingCandidate] = Field(min_length=2, max_length=100)

    @model_validator(mode="after")
    def validate_query(self) -> "RankingQuery":
        ids = [candidate.candidateId for candidate in self.candidates]
        widths = {len(candidate.featureValues) for candidate in self.candidates}
        if (
            self.occurredAt.tzinfo is None
            or self.outcomeObservedAt.tzinfo is None
            or self.outcomeObservedAt < self.occurredAt
            or len(ids) != len(set(ids))
            or len(widths) != 1
            or max(candidate.relevance for candidate in self.candidates) == 0
            or sum(candidate.converted for candidate in self.candidates) > 1
        ):
            raise ValueError("LTR_QUERY_INVALID")
        return self


class LearningToRankDataset(StrictContract):
    """Dataset temporal minimizado, versionado y purgado de revocaciones."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    purpose: Literal["offlineLearningToRankEvaluation"]
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    featureNames: list[Version] = Field(min_length=1, max_length=128)
    queries: list[RankingQuery] = Field(min_length=1, max_length=100_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "LearningToRankDataset":
        ids = [query.queryId for query in self.queries]
        if (
            self.extractedAt.tzinfo is None
            or len(ids) != len(set(ids))
            or len(self.featureNames) != len(set(self.featureNames))
            or any(query.outcomeObservedAt > self.extractedAt for query in self.queries)
            or any(
                len(candidate.featureValues) != len(self.featureNames)
                for query in self.queries
                for candidate in query.candidates
            )
        ):
            raise ValueError("LTR_DATASET_INVALID")
        return self


class RankingMetrics(StrictContract):
    """Métricas de utilidad y guardrails calculadas a la misma profundidad."""

    ndcgAtK: float = Field(ge=0, le=1)
    conversionAtK: float = Field(ge=0, le=1)
    diversityAtK: float = Field(ge=0, le=1)
    newVenueExposureAtK: float = Field(ge=0, le=1)


class LearningToRankReport(StrictContract):
    """Informe auditable de champion/challenger sin autoridad de despliegue."""

    modelVersion: Version
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    datasetVersion: Version
    evaluatedAt: datetime
    objective: Literal["rank:ndcg"]
    topK: int
    baselineMetrics: RankingMetrics
    candidateMetrics: RankingMetrics
    ndcgGain: float
    conversionGain: float
    diversityRegression: float
    newVenueExposureRegression: float
    stabilityMaximumDelta: float
    modelSha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    qualityGatesPassed: bool
    productionEvidence: bool
    promotionReviewAllowed: bool
    automaticDeploymentAllowed: Literal[False]
    modelCard: LearningToRankModelCard


class LearningToRankEvaluator:
    """Entrena XGBoost LambdaMART dos veces y conserva el baseline salvo mejora segura."""

    def __init__(self, policy: LearningToRankPolicy, model_card: LearningToRankModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            model_card.trainingPolicyVersion != policy.policyVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("LTR_MODEL_CARD_VERSION_MISMATCH")

    def evaluate(self, dataset: LearningToRankDataset) -> LearningToRankReport:
        """Compara listas futuras completas; evidencia sintética nunca habilita promoción."""
        self._validate_features(dataset.featureNames)
        train, evaluation = self._split(dataset.queries)
        self._validate_sample("TRAIN", train)
        self._validate_sample("EVALUATION", evaluation)
        first = self._fit(train)
        second = self._fit(train)
        candidate_scores = self._predict(first, evaluation)
        repeated_scores = self._predict(second, evaluation)
        baseline_scores = [[candidate.baselineScore for candidate in query.candidates] for query in evaluation]
        baseline_metrics = _ranking_metrics(evaluation, baseline_scores, self.policy.topK)
        candidate_metrics = _ranking_metrics(evaluation, candidate_scores, self.policy.topK)
        ndcg_gain = candidate_metrics.ndcgAtK - baseline_metrics.ndcgAtK
        conversion_gain = candidate_metrics.conversionAtK - baseline_metrics.conversionAtK
        diversity_regression = baseline_metrics.diversityAtK - candidate_metrics.diversityAtK
        exposure_regression = (
            baseline_metrics.newVenueExposureAtK - candidate_metrics.newVenueExposureAtK
        )
        stability = max(
            abs(value - repeated)
            for scores, repeated_query in zip(candidate_scores, repeated_scores, strict=True)
            for value, repeated in zip(scores, repeated_query, strict=True)
        )
        gates = (
            ndcg_gain >= self.policy.minimumNdcgGain
            and conversion_gain >= self.policy.minimumConversionGain
            and diversity_regression <= self.policy.maximumDiversityRegression
            and exposure_regression <= self.policy.maximumNewVenueExposureRegression
            and stability <= self.policy.maximumStabilityDelta
        )
        model_bytes = bytes(first.get_booster().save_raw(raw_format="json"))
        return LearningToRankReport(
            modelVersion=self.model_card.modelVersion,
            policyVersion=self.policy.policyVersion,
            algorithmVersion=self.policy.algorithmVersion,
            featureSetVersion=self.policy.featureSetVersion,
            datasetVersion=dataset.datasetVersion,
            evaluatedAt=dataset.extractedAt,
            objective="rank:ndcg",
            topK=self.policy.topK,
            baselineMetrics=baseline_metrics,
            candidateMetrics=candidate_metrics,
            ndcgGain=round(ndcg_gain, 8),
            conversionGain=round(conversion_gain, 8),
            diversityRegression=round(diversity_regression, 8),
            newVenueExposureRegression=round(exposure_regression, 8),
            stabilityMaximumDelta=round(stability, 12),
            modelSha256=hashlib.sha256(model_bytes).hexdigest(),
            qualityGatesPassed=gates,
            productionEvidence=dataset.productionEvidence,
            promotionReviewAllowed=gates and dataset.productionEvidence,
            automaticDeploymentAllowed=False,
            modelCard=self.model_card,
        )

    def _validate_features(self, feature_names: list[str]) -> None:
        prohibited = [fragment.casefold() for fragment in self.policy.prohibitedFeatureFragments]
        if any(fragment in name.casefold() for name in feature_names for fragment in prohibited):
            raise ValueError("LTR_PROHIBITED_FEATURE")

    def _split(self, queries: list[RankingQuery]):
        train = [query for query in queries if query.occurredAt < self.policy.trainingEndsBefore]
        evaluation = [
            query
            for query in queries
            if self.policy.trainingEndsBefore <= query.occurredAt < self.policy.evaluationEndsBefore
        ]
        if len(train) + len(evaluation) != len(queries):
            raise ValueError("LTR_QUERY_OUTSIDE_SPLIT")
        if any(query.outcomeObservedAt >= self.policy.trainingEndsBefore for query in train):
            raise ValueError("LTR_TRAIN_LABEL_NOT_MATURE")
        if any(query.outcomeObservedAt >= self.policy.evaluationEndsBefore for query in evaluation):
            raise ValueError("LTR_EVALUATION_LABEL_NOT_MATURE")
        return train, evaluation

    def _validate_sample(self, split: str, queries: list[RankingQuery]) -> None:
        if len(queries) < self.policy.minimumQueriesPerSplit or any(
            len(query.candidates) < self.policy.minimumCandidatesPerQuery for query in queries
        ):
            raise ValueError(f"LTR_{split}_SAMPLE_INSUFFICIENT")

    def _fit(self, queries: list[RankingQuery]) -> XGBRanker:
        ordered = sorted(queries, key=lambda query: (query.occurredAt, str(query.queryId)))
        features = np.asarray(
            [candidate.featureValues for query in ordered for candidate in query.candidates],
            dtype=np.float64,
        )
        labels = np.asarray(
            [candidate.relevance for query in ordered for candidate in query.candidates],
            dtype=np.int32,
        )
        groups = np.asarray([len(query.candidates) for query in ordered], dtype=np.uint32)
        model = XGBRanker(
            objective="rank:ndcg",
            eval_metric=f"ndcg@{self.policy.topK}",
            n_estimators=self.policy.nEstimators,
            max_depth=self.policy.maximumDepth,
            learning_rate=self.policy.learningRate,
            reg_lambda=self.policy.l2Penalty,
            subsample=1.0,
            colsample_bytree=1.0,
            tree_method="hist",
            random_state=self.policy.randomSeed,
            n_jobs=1,
            verbosity=0,
        )
        model.fit(features, labels, group=groups, verbose=False)
        return model

    @staticmethod
    def _predict(model: XGBRanker, queries: list[RankingQuery]) -> list[list[float]]:
        result: list[list[float]] = []
        for query in queries:
            values = np.asarray([candidate.featureValues for candidate in query.candidates], dtype=np.float64)
            result.append([float(value) for value in model.predict(values)])
        return result


def _ranking_metrics(
    queries: list[RankingQuery], scores_by_query: list[list[float]], top_k: int
) -> RankingMetrics:
    ndcg_values: list[float] = []
    conversions: list[float] = []
    diversities: list[float] = []
    exposures: list[float] = []
    for query, scores in zip(queries, scores_by_query, strict=True):
        order = sorted(
            range(len(query.candidates)),
            key=lambda index: (-scores[index], str(query.candidates[index].candidateId)),
        )
        top = order[:top_k]
        ideal = sorted(
            range(len(query.candidates)),
            key=lambda index: (-query.candidates[index].relevance, str(query.candidates[index].candidateId)),
        )[:top_k]
        dcg = _dcg([query.candidates[index].relevance for index in top])
        ideal_dcg = _dcg([query.candidates[index].relevance for index in ideal])
        ndcg_values.append(dcg / ideal_dcg)
        conversions.append(float(any(query.candidates[index].converted for index in top)))
        diversities.append(len({query.candidates[index].categoryCode for index in top}) / len(top))
        exposures.append(sum(query.candidates[index].isNewVenue for index in top) / len(top))
    return RankingMetrics(
        ndcgAtK=round(float(np.mean(ndcg_values)), 8),
        conversionAtK=round(float(np.mean(conversions)), 8),
        diversityAtK=round(float(np.mean(diversities)), 8),
        newVenueExposureAtK=round(float(np.mean(exposures)), 8),
    )


def _dcg(relevances: list[int]) -> float:
    return sum((2**relevance - 1) / math.log2(position + 2) for position, relevance in enumerate(relevances))
