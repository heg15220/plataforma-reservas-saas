"""Pruebas de uplift, margen, aprobación y frecuencia para promociones inteligentes."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import UUID, uuid4

from reserly_demand_engine.smart_promotions import (
    SmartPromotionPlanner,
    SmartPromotionPolicy,
    SmartPromotionRequest,
)


POLICY_PATH = Path(__file__).resolve().parents[1] / "policies" / "smart-promotion.v1.json"


class SmartPromotionTests(unittest.TestCase):
    """Acredita que ningún descuento cruza una puerta causal u operativa fallida."""

    def setUp(self) -> None:
        self.now = datetime(2026, 8, 21, 9, 0, tzinfo=UTC)
        self.slot_id = uuid4()
        self.planner = SmartPromotionPlanner(SmartPromotionPolicy.load(POLICY_PATH))

    def _candidate(self, **changes: object) -> dict[str, object]:
        candidate: dict[str, object] = {
            "promotionId": str(uuid4()),
            "contactSubjectId": str(uuid4()),
            "venueId": str(uuid4()),
            "timeSlotId": str(self.slot_id),
            "createdAt": self.now.isoformat(),
            "baselineBookingProbability": 0.25,
            "attendanceProbability": 0.9,
            "projectedNetMarginCents": 5_000,
            "discountCostCents": 300,
            "contactCostCents": 10,
            "contactConsent": True,
            "contactsInWindow": 0,
            "venueApprovalId": str(uuid4()),
            "venueApproved": True,
            "venueApprovedMaximumDiscountCents": 500,
            "venueApprovalExpiresAt": (self.now + timedelta(days=1)).isoformat(),
            "uplift": {
                "modelVersion": "uplift-doubly-robust-v1",
                "policyVersion": "uplift-doubly-robust-v1",
                "estimate": 0.12,
                "confidenceLower": 0.08,
                "confidenceUpper": 0.16,
                "overlapGatesPassed": True,
                "signStableUnderSensitivity": True,
                "productionEvidence": True,
                "causalInterpretationAllowed": True,
                "upliftActionReviewAllowed": True,
                "observationalAttributionUsedForUplift": False,
            },
            "constraints": {
                "venuePublished": True,
                "serviceBookable": True,
                "eligibilityAllowed": True,
                "permissionAllowed": True,
                "filtersMatched": True,
                "frequencyAllowed": True,
                "availableCapacity": 1,
                "requestedCapacity": 1,
                "validUntil": (self.now + timedelta(hours=1)).isoformat(),
            },
        }
        for key, value in changes.items():
            if key.startswith("uplift_"):
                candidate["uplift"][key.removeprefix("uplift_")] = value
            elif key.startswith("constraint_"):
                candidate["constraints"][key.removeprefix("constraint_")] = value
            else:
                candidate[key] = value
        return candidate

    def _request(
        self, candidates: list[dict[str, object]], budget: int = 1_000
    ) -> SmartPromotionRequest:
        return SmartPromotionRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": self.now.isoformat(),
                "locale": "es",
                "policyVersion": "smart-promotion-v1",
                "budgetCents": budget,
                "candidates": candidates,
            }
        )

    def test_selects_highest_incremental_value_with_budget_and_capacity(self) -> None:
        low = self._candidate(projectedNetMarginCents=2_000)
        high = self._candidate(projectedNetMarginCents=8_000)
        result = self.planner.plan(self._request([low, high]))
        self.assertEqual("optimal", result.status)
        self.assertEqual(1, result.selectedCount)
        self.assertEqual(UUID(high["promotionId"]), result.selections[0].promotionId)
        self.assertFalse(result.automaticContactAllowed)

    def test_unreliable_uplift_blocks_promotions_instead_of_fallback_discount(self) -> None:
        candidate = self._candidate(uplift_productionEvidence=False)
        result = self.planner.plan(self._request([candidate]))
        self.assertEqual("blockedUnreliable", result.status)
        self.assertEqual(0, result.selectedCount)
        self.assertEqual(1, result.exclusionCounts["reliableUpliftRequired"])

    def test_likely_natural_booker_is_not_discounted(self) -> None:
        result = self.planner.plan(
            self._request([self._candidate(baselineBookingProbability=0.9)])
        )
        self.assertEqual(1, result.exclusionCounts["likelyWithoutIncentive"])
        self.assertEqual(0, result.selectedCount)

    def test_margin_approval_consent_and_frequency_are_hard_gates(self) -> None:
        candidates = [
            self._candidate(projectedNetMarginCents=50),
            self._candidate(discountCostCents=600),
            self._candidate(contactConsent=False),
            self._candidate(contactsInWindow=3),
        ]
        result = self.planner.plan(self._request(candidates))
        self.assertEqual(0, result.selectedCount)
        self.assertEqual(1, result.exclusionCounts["marginFloor"])
        self.assertEqual(1, result.exclusionCounts["venueApprovalRequired"])
        self.assertEqual(1, result.exclusionCounts["consentRequired"])
        self.assertEqual(1, result.exclusionCounts["frequencyLimit"])

    def test_same_subject_never_receives_two_promotions(self) -> None:
        subject_id = str(uuid4())
        first = self._candidate(contactSubjectId=subject_id, projectedNetMarginCents=8_000)
        second = self._candidate(contactSubjectId=subject_id, projectedNetMarginCents=7_000)
        result = self.planner.plan(self._request([first, second], budget=2_000))
        self.assertEqual(1, result.selectedCount)
        self.assertEqual(UUID(first["promotionId"]), result.selections[0].promotionId)

    def test_expired_approval_and_invalid_constraint_are_rejected(self) -> None:
        expired = self._candidate(venueApprovalExpiresAt=self.now.isoformat())
        blocked = self._candidate(constraint_serviceBookable=False)
        result = self.planner.plan(self._request([expired, blocked]))
        self.assertEqual(1, result.exclusionCounts["venueApprovalRequired"])
        self.assertEqual(1, result.exclusionCounts["hardConstraint"])


if __name__ == "__main__":
    unittest.main()
