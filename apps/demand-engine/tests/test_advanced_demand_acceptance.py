"""Puerta de release de optimización, seguridad causal, drift y degradación."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

from reserly_demand_engine.advanced_acceptance import AdvancedDemandAcceptanceMatrix


ROOT = Path(__file__).resolve().parents[1]


class AdvancedDemandAcceptanceTests(unittest.TestCase):
    """Impide cerrar Fase 22 si desaparece una evidencia o una salida fail-closed."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.matrix = AdvancedDemandAcceptanceMatrix.load(
            ROOT / "evaluation/advanced-demand-acceptance.v1.json"
        )

    def test_matrix_covers_all_eight_advanced_risk_categories(self) -> None:
        self.assertEqual(8, len(set(self.matrix.requiredCategories)))
        self.assertEqual(16, len(self.matrix.checks))
        counts = {category: 0 for category in self.matrix.requiredCategories}
        for check in self.matrix.checks:
            counts[check.category] += 1
        self.assertTrue(all(count == 2 for count in counts.values()))

    def test_every_acceptance_reference_exists_in_discovered_tests(self) -> None:
        self.matrix.validate_test_references(ROOT / "tests")

    def test_material_actions_remain_non_automatic_in_every_policy(self) -> None:
        expectations = {
            "opportunity-optimization.v1.json": ("automaticExecutionAllowed", False),
            "waitlist-allocation.v1.json": ("automaticExecutionAllowed", False),
            "smart-promotion.v1.json": ("automaticContactAllowed", False),
            "incremental-learning.v1.json": ("automaticPromotionAllowed", False),
            "incrementality-measurement.v1.json": ("automaticCommercialClaimAllowed", False),
        }
        for filename, (field, expected) in expectations.items():
            policy = json.loads((ROOT / "policies" / filename).read_text(encoding="utf-8"))
            self.assertEqual(expected, policy[field], filename)

    def test_drift_policy_and_model_card_define_same_rollback_target(self) -> None:
        policy = json.loads(
            (ROOT / "policies/incremental-learning.v1.json").read_text(encoding="utf-8")
        )
        card = json.loads(
            (ROOT / "models/incremental-logistic-shadow.v1.json").read_text(encoding="utf-8")
        )
        self.assertIn(policy["fallbackPolicyVersion"], card["rollback"])
        self.assertFalse(card["automaticPromotionAllowed"])
        self.assertTrue(card["humanApprovalRequired"])

    def test_failure_responses_are_fail_closed_for_each_category(self) -> None:
        forbidden = {"continue", "ignore", "automaticRetry", "automaticPromotion"}
        for check in self.matrix.checks:
            self.assertNotIn(check.failureResponse, forbidden)


if __name__ == "__main__":
    unittest.main()
