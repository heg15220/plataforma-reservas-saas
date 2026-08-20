"""Pruebas de conjuntos completos, signos, opción exterior y evaluación temporal."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.discrete_choice import (
    ChoiceDataset,
    ChoiceSet,
    ConditionalLogitTrainer,
    DiscreteChoiceModelCard,
    DiscreteChoicePolicy,
)


ROOT = Path(__file__).parents[1]


class DiscreteChoiceTests(unittest.TestCase):
    """Protege interpretación condicional sin reintroducir alternativas no observadas."""

    def setUp(self) -> None:
        policy = DiscreteChoicePolicy.load(
            ROOT / "policies" / "discrete-choice-training.v1.json"
        )
        card = DiscreteChoiceModelCard.load(
            ROOT / "models" / "discrete-choice-baseline.v1.model-card.json"
        )
        self.trainer = ConditionalLogitTrainer(policy, card)

    def _dataset(self, production: bool = False) -> ChoiceDataset:
        features = [
            "distanceKm",
            "priceTenEur",
            "attributeMatch",
            "availability",
            "contextMatch",
        ]
        negative = {"distanceKm", "priceTenEur"}
        sets: list[dict[str, object]] = []
        for start in (datetime(2026, 4, 1, tzinfo=UTC), datetime(2026, 5, 1, tzinfo=UTC)):
            for index in range(25):
                code = features[index % len(features)]
                preferred_id, other_id = uuid4(), uuid4()
                preferred = {item: 0.5 for item in features}
                other = {item: 0.5 for item in features}
                preferred[code] = 0.0 if code in negative else 1.0
                other[code] = 1.0 if code in negative else 0.0
                sets.append(
                    {
                        "choiceSetId": str(uuid4()),
                        "occurredAt": start + timedelta(hours=index),
                        "fullChoiceSetCaptured": True,
                        "candidateCount": 2,
                        "alternatives": [
                            {
                                "alternativeId": str(preferred_id),
                                "eligible": True,
                                "capacityAvailable": True,
                                "features": preferred,
                            },
                            {
                                "alternativeId": str(other_id),
                                "eligible": True,
                                "capacityAvailable": True,
                                "features": other,
                            },
                        ],
                        "chosenAlternativeId": str(preferred_id),
                        "outsideOptionChosen": False,
                    }
                )
        return ChoiceDataset.model_validate(
            {
                "datasetVersion": "choice-synthetic-contract-v1",
                "extractedAt": datetime(2026, 7, 1, tzinfo=UTC),
                "currency": "EUR",
                "productionEvidence": production,
                "containsPersonalData": False,
                "consentRevocationsApplied": True,
                "purpose": "analytics",
                "choiceSets": sets,
            }
        )

    def test_trains_expected_signs_and_future_fit_deterministically(self) -> None:
        dataset = self._dataset()
        first = self.trainer.train(dataset)
        second = self.trainer.train(dataset)
        self.assertEqual(first, second)
        self.assertTrue(all(item.directionMatches for item in first.interpretations))
        self.assertLess(first.coefficients["distanceKm"], 0)
        self.assertLess(first.coefficients["priceTenEur"], 0)
        self.assertGreater(first.coefficients["attributeMatch"], 0)
        self.assertGreaterEqual(first.evaluationMetrics.topOneAccuracy, 0.9)
        self.assertTrue(first.gatesPassed)
        self.assertFalse(first.promotionAllowed)

    def test_probabilities_include_outside_and_preserve_alternatives(self) -> None:
        artifact = self.trainer.train(self._dataset(production=True))
        choice_set = self._dataset().choiceSets[-1]
        probabilities = artifact.probabilities(choice_set)
        self.assertEqual(3, len(probabilities))
        self.assertIsNone(probabilities[-1].alternativeId)
        self.assertAlmostEqual(1, sum(item.probability for item in probabilities), places=10)
        self.assertEqual(choice_set.chosenAlternativeId, probabilities[0].alternativeId)
        self.assertGreater(probabilities[0].probability, probabilities[1].probability)

    def test_rejects_truncated_duplicate_and_uncaptured_choices(self) -> None:
        raw = self._dataset().choiceSets[0].model_dump()
        raw["candidateCount"] = 3
        with self.assertRaises(ValidationError):
            ChoiceSet.model_validate(raw)
        raw = self._dataset().choiceSets[0].model_dump()
        raw["chosenAlternativeId"] = uuid4()
        with self.assertRaises(ValidationError):
            ChoiceSet.model_validate(raw)
        raw = self._dataset().choiceSets[0].model_dump()
        raw["fullChoiceSetCaptured"] = False
        with self.assertRaises(ValidationError):
            ChoiceSet.model_validate(raw)

    def test_rejects_position_and_outcome_leakage(self) -> None:
        raw = self._dataset().model_dump()
        raw["choiceSets"][0]["alternatives"][0]["features"]["displayedPosition"] = 1
        dataset = ChoiceDataset.model_validate(raw)
        with self.assertRaises(ValueError):
            self.trainer.train(dataset)
        raw = self._dataset().model_dump()
        raw["choiceSets"][0]["alternatives"][0]["features"]["bookingCompleted"] = 1
        dataset = ChoiceDataset.model_validate(raw)
        with self.assertRaises(ValueError):
            self.trainer.train(dataset)

    def test_rejects_insufficient_temporal_evaluation_sample(self) -> None:
        raw = self._dataset().model_dump()
        raw["choiceSets"] = raw["choiceSets"][:40]
        dataset = ChoiceDataset.model_validate(raw)
        with self.assertRaises(ValueError):
            self.trainer.train(dataset)


if __name__ == "__main__":
    unittest.main()
