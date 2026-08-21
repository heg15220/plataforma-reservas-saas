import json
import tempfile
import unittest
from pathlib import Path

from reserly_demand_engine.governance_documentation import validate_governance_documentation


ROOT = Path(__file__).parents[1]


class GovernanceDocumentationTests(unittest.TestCase):
    """Prueba la puerta documental sin otorgarle autoridad de promoción."""

    def test_repository_bundle_is_complete_and_content_addressed(self) -> None:
        evidence = validate_governance_documentation(ROOT)

        self.assertGreaterEqual(evidence.modelCardCount, 8)
        self.assertEqual(3, evidence.dataSheetCount)
        self.assertTrue(evidence.documentationComplete)
        self.assertTrue(evidence.legalApprovalRequired)
        self.assertFalse(evidence.promotionAuthorized)
        self.assertTrue(all(len(value) == 64 for value in evidence.modelCardSha256.values()))

    def test_prohibited_matrix_covers_all_sensitive_families(self) -> None:
        matrix = json.loads((ROOT / "governance" / "prohibited-attributes.v1.json").read_text())
        categories = {item["category"] for item in matrix["prohibited"]}

        self.assertEqual({"directIdentifier", "tracking", "sensitiveInference"}, categories)
        self.assertFalse(matrix["exceptionsAllowed"])
        self.assertFalse(matrix["automaticRelaxationAllowed"])

    def test_missing_model_card_field_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "demand-engine"
            self._copy_bundle(target)
            card_path = next((target / "models").glob("*.model-card.json"))
            card = json.loads(card_path.read_text())
            del card["rollback"]
            card_path.write_text(json.dumps(card), encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "MODEL_CARD_INCOMPLETE"):
                validate_governance_documentation(target)

    def test_premature_legal_approval_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "demand-engine"
            self._copy_bundle(target)
            pia_path = target / "governance" / "privacy-impact-assessment.v1.json"
            pia = json.loads(pia_path.read_text())
            pia["status"] = "approved"
            pia_path.write_text(json.dumps(pia), encoding="utf-8")

            with self.assertRaises(ValueError):
                validate_governance_documentation(target)

    def _copy_bundle(self, target: Path) -> None:
        import shutil

        shutil.copytree(ROOT / "governance", target / "governance")
        shutil.copytree(ROOT / "models", target / "models")
        shutil.copytree(ROOT / "policies", target / "policies")


if __name__ == "__main__":
    unittest.main()
