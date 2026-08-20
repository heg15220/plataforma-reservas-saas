"""Pruebas de ABSA por aspecto, vigencia, revisión y evaluación humana."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.absa import (
    AbsaEvaluationRequest,
    ReviewAbsaAnalyzer,
    ReviewAbsaPolicy,
    VerifiedReviewRequest,
)


POLICY = Path(__file__).parents[1] / "policies" / "review-absa.v1.json"


class ReviewAbsaTests(unittest.TestCase):
    """Protege aspectos separados y evita convertir estrellas en atributos no mencionados."""

    def setUp(self) -> None:
        self.now = datetime(2026, 8, 20, 12, tzinfo=UTC)
        self.analyzer = ReviewAbsaAnalyzer(ReviewAbsaPolicy.load(POLICY))

    def _review(self, text: str, locale: str = "es", rating: int = 5) -> VerifiedReviewRequest:
        return VerifiedReviewRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": self.now,
                "locale": locale,
                "policyVersion": "review-absa-v1",
                "reviewId": str(uuid4()),
                "venueId": str(uuid4()),
                "verifiedReservation": True,
                "rating": rating,
                "text": text,
            }
        )

    def test_separates_aspects_confidence_and_validity(self) -> None:
        result = self.analyzer.analyze(
            self._review("El trato fue amable y atento, pero el ambiente era ruidoso y malo")
        )
        aspects = {item.aspectCode: item for item in result.aspects}
        self.assertGreater(aspects["staffAttentiveness"].score, 0)
        self.assertLess(aspects["calmAtmosphere"].score, 0)
        self.assertEqual("machineAccepted", aspects["staffAttentiveness"].reviewStatus)
        self.assertEqual(self.now + timedelta(days=90), aspects["calmAtmosphere"].expiresAt)

    def test_negation_flips_sentiment_and_contradiction_requires_human(self) -> None:
        negative = self.analyzer.analyze(self._review("La cita no fue puntual"))
        self.assertLess(negative.aspects[0].score, 0)
        contradictory = self.analyzer.analyze(
            self._review("Trato amable y brusco")
        )
        self.assertEqual(0, contradictory.aspects[0].score)
        self.assertEqual("pendingHuman", contradictory.aspects[0].reviewStatus)

    def test_rating_does_not_fabricate_an_unmentioned_aspect_and_output_has_no_text(self) -> None:
        result = self.analyzer.analyze(self._review("Una experiencia excelente", rating=5))
        self.assertEqual([], result.aspects)
        serialized = result.model_dump_json().casefold()
        self.assertNotIn("text", serialized)
        self.assertNotIn("comment", serialized)
        self.assertNotIn("rating", serialized)

    def test_english_synonyms_are_supported(self) -> None:
        result = self.analyzer.analyze(
            self._review("Friendly and attentive staff, but a noisy atmosphere", "en")
        )
        aspects = {item.aspectCode: item.score for item in result.aspects}
        self.assertGreater(aspects["staffAttentiveness"], 0)
        self.assertLess(aspects["calmAtmosphere"], 0)

    def test_human_evaluation_applies_sample_accuracy_and_mae_gates(self) -> None:
        prediction = self.analyzer.analyze(self._review("El trato fue amable y atento"))
        aspect = prediction.aspects[0]
        cases = [
            {
                "reviewId": str(uuid4()),
                "predicted": [aspect.model_dump()],
                "humanLabels": [{"aspectCode": aspect.aspectCode, "score": 1}],
            }
            for _ in range(20)
        ]
        request = AbsaEvaluationRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": self.now,
                "locale": "es",
                "policyVersion": "review-absa-v1",
                "datasetVersion": "human-absa-es-en-v1",
                "cases": cases,
            }
        )
        result = self.analyzer.evaluate(request)
        self.assertTrue(result.promotable)
        self.assertEqual(1, result.polarityAccuracy)
        self.assertEqual(0, result.macroMae)

    def test_policy_drift_and_unverified_reviews_fail_closed(self) -> None:
        raw = self._review("El trato fue bueno").model_dump()
        raw["verifiedReservation"] = False
        with self.assertRaises(ValueError):
            VerifiedReviewRequest.model_validate(raw)
        request = self._review("El trato fue bueno").model_copy(
            update={"policyVersion": "unknown-v1"}
        )
        with self.assertRaises(ValueError):
            self.analyzer.analyze(request)


if __name__ == "__main__":
    unittest.main()
