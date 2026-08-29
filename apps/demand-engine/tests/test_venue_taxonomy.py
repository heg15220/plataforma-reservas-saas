"""Pruebas de reutilización segura de las 200 imágenes existentes."""

from __future__ import annotations

import hashlib
import json
import tempfile
import unittest
from collections import Counter
from pathlib import Path

from reserly_demand_engine.venue_taxonomy import build_relabel_bundle, write_relabel_bundle


ROOT = Path(__file__).parents[1]
TAXONOMY = ROOT.parents[1] / "packages/demand-contracts/catalog/venue-taxonomy.v1.json"
DEFINITION = ROOT / "evaluation/synthetic-marketplace-v1/visual-training-dataset-v2/approved-definition.json"


class VenueTaxonomyRelabelTests(unittest.TestCase):
    """Garantiza que reetiquetar no fabrique imágenes ni recicle un test consumido."""

    def test_builds_200_pending_development_only_rows(self) -> None:
        bundle = build_relabel_bundle(TAXONOMY, DEFINITION)
        self.assertEqual(200, len(bundle.rows))
        self.assertEqual({25}, set(Counter(item.legacyCategoryCode for item in bundle.rows).values()))
        self.assertTrue(all(item.reviewStatus == "pendingHumanReview" for item in bundle.rows))
        self.assertTrue(all(item.allowedUse == "developmentRelabelingOnly" for item in bundle.rows))
        self.assertFalse(any(item.testEligible for item in bundle.rows))
        self.assertEqual(0, bundle.manifest["newImagesGenerated"])
        municipal = [item for item in bundle.rows if item.legacyCategoryCode == "instalacion-municipal"]
        self.assertTrue(all(not item.proposedTypeCodes for item in municipal))
        self.assertTrue(all(item.proposedOperatorTypeCode == "public-municipal" for item in municipal))

    def test_written_worklist_matches_manifest_hash(self) -> None:
        bundle = build_relabel_bundle(TAXONOMY, DEFINITION)
        with tempfile.TemporaryDirectory() as directory:
            worklist = Path(directory) / "worklist.jsonl"
            manifest = Path(directory) / "manifest.json"
            write_relabel_bundle(bundle, worklist, manifest)
            persisted = json.loads(manifest.read_text(encoding="utf-8"))
            self.assertEqual(hashlib.sha256(worklist.read_bytes()).hexdigest(), persisted["worklistSha256"])
            self.assertEqual(200, len(worklist.read_text(encoding="utf-8").splitlines()))

    def test_rejects_unapproved_or_duplicate_source_rows(self) -> None:
        payload = json.loads(DEFINITION.read_text(encoding="utf-8"))
        payload["rows"][0]["humanReviewStatus"] = "pending"
        with tempfile.TemporaryDirectory() as directory:
            invalid = Path(directory) / "invalid.json"
            invalid.write_text(json.dumps(payload), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "RELABEL_ROW_INVALID"):
                build_relabel_bundle(TAXONOMY, invalid)


if __name__ == "__main__":
    unittest.main()
