"""Puerta transversal de reproducibilidad, leakage, calibración, sesgo, idioma y promoción."""

from __future__ import annotations

import json
import unittest
from pathlib import Path
from typing import get_args

from reserly_demand_engine.attribute_discovery import AttributeDiscoveryDataset
from reserly_demand_engine.conversion_analytics import ConversionAnalyticsDataset
from reserly_demand_engine.conversion_training import ConversionDataset
from reserly_demand_engine.discrete_choice import ChoiceDataset
from reserly_demand_engine.governance_acceptance import GovernanceAcceptanceMatrix
from reserly_demand_engine.no_show_risk import NoShowDataset

ROOT = Path(__file__).parents[1]


class ModelGovernanceAcceptanceTests(unittest.TestCase):
    """Impide cerrar la fase si una dimensión o evidencia gobernada desaparece."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.matrix = GovernanceAcceptanceMatrix.load(
            ROOT / "evaluation" / "model-governance-acceptance.v1.json"
        )

    def test_matrix_covers_all_seven_risk_categories(self) -> None:
        self.assertEqual(7, len(set(self.matrix.requiredCategories)))
        self.assertGreaterEqual(len(self.matrix.checks), 14)

    def test_every_matrix_reference_exists_in_discovered_unittest_files(self) -> None:
        self.matrix.validate_test_references(ROOT / "tests")

    def test_feature_policies_exclude_sensitive_identity_and_outcome_proxies(self) -> None:
        policy_files = [
            "conversion-logistic-training.v1.json",
            "discrete-choice-training.v1.json",
            "no-show-risk-training.v1.json",
        ]
        forbidden = {
            "customeremail", "customerid", "reservationid", "gender", "age", "health",
            "postcode", "paymentmethod", "converted", "noshow", "attendanceoutcome",
        }
        for filename in policy_files:
            policy = json.loads((ROOT / "policies" / filename).read_text(encoding="utf-8"))
            features = {value.casefold() for value in policy["featureCodes"]}
            self.assertTrue(features.isdisjoint(forbidden), filename)
            self.assertTrue(features.isdisjoint({value.casefold() for value in policy["prohibitedFeatureCodes"]}), filename)

    def test_calibration_and_bias_gates_are_bounded_and_sampled(self) -> None:
        conversion = json.loads((ROOT / "policies" / "conversion-logistic-training.v1.json").read_text(encoding="utf-8"))
        no_show = json.loads((ROOT / "policies" / "no-show-risk-training.v1.json").read_text(encoding="utf-8"))
        boosting = json.loads((ROOT / "policies" / "boosting-comparison.v1.json").read_text(encoding="utf-8"))
        self.assertLessEqual(conversion["evaluationGates"]["maximumExpectedCalibrationError"], 0.15)
        self.assertLessEqual(no_show["maximumExpectedCalibrationError"], 0.15)
        self.assertLessEqual(no_show["maximumSegmentBrierGap"], 0.05)
        self.assertLessEqual(boosting["maximumSegmentBrierGap"], 0.05)
        self.assertGreaterEqual(no_show["minimumRowsPerAuditSegment"], 10)
        self.assertGreaterEqual(boosting["minimumRowsPerAuditSegment"], 10)

    def test_linguistic_policies_require_spanish_and_english(self) -> None:
        nlp = json.loads((ROOT / "policies" / "nlp-personal-care.v1.json").read_text(encoding="utf-8"))
        absa = json.loads((ROOT / "policies" / "review-absa.v1.json").read_text(encoding="utf-8"))
        discovery = json.loads((ROOT / "policies" / "attribute-discovery.v1.json").read_text(encoding="utf-8"))
        self.assertEqual({"es", "en"}, set(nlp["negators"]))
        self.assertEqual({"es", "en"}, set(absa["negators"]))
        self.assertGreaterEqual(discovery["minimumDocumentsPerLocale"], 2)

    def test_revocation_is_mandatory_across_governed_datasets(self) -> None:
        datasets = (
            ConversionDataset,
            ChoiceDataset,
            NoShowDataset,
            AttributeDiscoveryDataset,
            ConversionAnalyticsDataset,
        )
        for dataset in datasets:
            annotation = dataset.model_fields["consentRevocationsApplied"].annotation
            self.assertEqual((True,), get_args(annotation), dataset.__name__)

    def test_model_cards_and_policies_require_human_promotion(self) -> None:
        cards = list((ROOT / "models").glob("*.model-card.json"))
        self.assertGreaterEqual(len(cards), 4)
        for path in cards:
            card = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual("candidate", card["status"], path.name)
            self.assertTrue(card["humanApprovalRequired"], path.name)
            self.assertTrue(card["rollback"], path.name)
        discovery = json.loads((ROOT / "policies" / "attribute-discovery.v1.json").read_text(encoding="utf-8"))
        self.assertFalse(discovery["automaticPublicationAllowed"])
        self.assertEqual("ROLE_ADMIN", discovery["requiredReviewRole"])


if __name__ == "__main__":
    unittest.main()
