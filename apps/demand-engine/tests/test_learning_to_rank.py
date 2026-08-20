"""Pruebas de LambdaMART real, listas completas, métricas y promoción gobernada."""

from __future__ import annotations

import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.learning_to_rank import (
    LearningToRankDataset,
    LearningToRankEvaluator,
    LearningToRankModelCard,
    LearningToRankPolicy,
    RankingCandidate,
    RankingQuery,
)


ROOT = Path(__file__).resolve().parents[1]
UTC = timezone.utc


def _query(day: datetime, offset: int) -> RankingQuery:
    candidates = []
    for index in range(6):
        relevance = 3 - index if index < 4 else 0
        candidates.append(
            RankingCandidate(
                candidateId=uuid4(),
                featureValues=[relevance / 3, float(index == 1), (offset % 3) / 2],
                baselineScore=float(index),
                relevance=relevance,
                converted=int(index == 0),
                categoryCode=f"category{index % 3}",
                isNewVenue=index == 1,
                eligible=True,
                capacityAvailable=True,
            )
        )
    return RankingQuery(
        queryId=uuid4(),
        occurredAt=day + timedelta(minutes=offset),
        outcomeObservedAt=day + timedelta(minutes=offset + 1),
        completeCandidateSet=True,
        candidates=candidates,
    )


def _dataset(production: bool = False) -> LearningToRankDataset:
    train_day = datetime(2026, 5, 1, tzinfo=UTC)
    evaluation_day = datetime(2026, 6, 5, tzinfo=UTC)
    return LearningToRankDataset(
        datasetVersion="ltr-fixture-v1",
        extractedAt=datetime(2026, 7, 2, tzinfo=UTC),
        productionEvidence=production,
        purpose="offlineLearningToRankEvaluation",
        containsPersonalData=False,
        consentRevocationsApplied=True,
        featureNames=["contentAffinity", "newVenue", "contextMatch"],
        queries=[*[_query(train_day, i) for i in range(12)], *[_query(evaluation_day, i) for i in range(12)]],
    )


class LearningToRankTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = LearningToRankPolicy.load(ROOT / "policies" / "learning-to-rank-evaluation.v1.json")
        cls.card = LearningToRankModelCard.load(ROOT / "models" / "learning-to-rank-candidate.v1.model-card.json")
        cls.evaluator = LearningToRankEvaluator(cls.policy, cls.card)

    def test_lambdamart_improves_ndcg_conversion_and_preserves_guardrails(self) -> None:
        report = self.evaluator.evaluate(_dataset())
        self.assertEqual(report.objective, "rank:ndcg")
        self.assertGreaterEqual(report.ndcgGain, self.policy.minimumNdcgGain)
        self.assertGreaterEqual(report.conversionGain, 0)
        self.assertLessEqual(report.diversityRegression, 0)
        self.assertLessEqual(report.newVenueExposureRegression, 0)
        self.assertTrue(report.qualityGatesPassed)
        self.assertFalse(report.promotionReviewAllowed)
        self.assertFalse(report.automaticDeploymentAllowed)

    def test_production_evidence_allows_only_human_review(self) -> None:
        report = self.evaluator.evaluate(_dataset(production=True))
        self.assertTrue(report.promotionReviewAllowed)
        self.assertTrue(report.modelCard.humanApprovalRequired)
        self.assertFalse(report.automaticDeploymentAllowed)
        self.assertEqual(len(report.modelSha256), 64)

    def test_sensitive_or_outcome_feature_is_rejected(self) -> None:
        dataset = _dataset()
        dataset.featureNames[0] = "customerAge"
        with self.assertRaisesRegex(ValueError, "LTR_PROHIBITED_FEATURE"):
            self.evaluator.evaluate(dataset)

    def test_incomplete_or_ineligible_candidate_fails_contract(self) -> None:
        candidate = _dataset().queries[0].candidates[0].model_dump(mode="json")
        candidate["eligible"] = False
        with self.assertRaises(ValidationError):
            RankingCandidate.model_validate(candidate)

        query = _dataset().queries[0].model_dump(mode="json")
        query["completeCandidateSet"] = False
        with self.assertRaises(ValidationError):
            RankingQuery.model_validate(query)

    def test_mature_labels_and_minimum_queries_are_enforced(self) -> None:
        immature = _dataset()
        immature.queries[0] = immature.queries[0].model_copy(
            update={"outcomeObservedAt": datetime(2026, 6, 1, tzinfo=UTC)}
        )
        with self.assertRaisesRegex(ValueError, "LTR_TRAIN_LABEL_NOT_MATURE"):
            self.evaluator.evaluate(immature)

        insufficient = _dataset()
        insufficient = insufficient.model_copy(
            update={"queries": insufficient.queries[:8] + insufficient.queries[12:20]}
        )
        with self.assertRaisesRegex(ValueError, "LTR_TRAIN_SAMPLE_INSUFFICIENT"):
            self.evaluator.evaluate(insufficient)


if __name__ == "__main__":
    unittest.main()
