"""Pruebas estructurales de la QA visual sintética."""

from __future__ import annotations

import hashlib
import json
import random
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

from reserly_demand_engine.synthetic_visual_qa import (
    CATEGORY_PROMPTS,
    _cohort_classification_summaries,
    inspect_assets,
    inspect_holdout_assets,
)

ROOT = Path(__file__).resolve().parents[1]


class SyntheticVisualQaTests(unittest.TestCase):
    def _fixture(self, root: Path, duplicate: bool = False) -> None:
        (root / "images").mkdir()
        venues = []
        prompts = []
        index = 0
        for cohort in ("warm", "validationCold", "testCold"):
            for category in CATEGORY_PROMPTS:
                index += 1
                venues.append({"venueId": f"venue-{index}", "categoryCode": category, "entityCohort": cohort})
                prompts.append({"imagePromptId": f"prompt-{index}", "promptVersion": "v1"})
                generator = random.Random(1 if duplicate and index <= 2 else index)
                small = Image.new("L", (9, 8))
                small.putdata([generator.randrange(256) for _ in range(72)])
                image = small.resize((1024, 768)).convert("RGB")
                draw = ImageDraw.Draw(image)
                draw.rectangle((index, index, 500 + index, 400 + index), outline="white", width=5)
                image.save(root / "images" / f"venue-{index:03}.png")
        (root / "venues.jsonl").write_text("".join(json.dumps(row) + "\n" for row in venues), encoding="utf-8")
        (root / "image-prompts.jsonl").write_text("".join(json.dumps(row) + "\n" for row in prompts), encoding="utf-8")

    def test_complete_fixture_covers_every_category_and_cohort(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self._fixture(root)
            assets, report = inspect_assets(root)
            self.assertEqual(24, len(assets))
            self.assertTrue(report["passed"], report)
            self.assertEqual(set(CATEGORY_PROMPTS), set(report["categoryCounts"]))

    def test_missing_image_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self._fixture(root)
            (root / "images" / "venue-024.png").unlink()
            _, report = inspect_assets(root)
            self.assertFalse(report["passed"])
            self.assertIn("IMAGE_MISSING", {item["code"] for item in report["violations"]})

    def test_versioned_replacement_is_selected_without_overwriting_original(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self._fixture(root)
            (root / "images-v2").mkdir()
            replacement = root / "images-v2" / "venue-001-v2.png"
            Image.new("RGB", (1024, 768), "navy").save(replacement)
            original_sha = hashlib.sha256(
                (root / "images" / "venue-001.png").read_bytes()
            ).hexdigest()
            assets, report = inspect_assets(root, {"venue-001.png": "images-v2/venue-001-v2.png"})
            self.assertTrue(report["passed"], report)
            self.assertIn("images-v2/venue-001-v2.png", assets[0]["objectKey"])
            self.assertNotEqual(original_sha, assets[0]["sha256"])
            self.assertTrue((root / "images" / "venue-001.png").is_file())

    def test_versioned_replacement_cannot_escape_dataset(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self._fixture(root)
            with self.assertRaisesRegex(ValueError, "VISUAL_ASSET_PATH_INVALID"):
                inspect_assets(root, {"venue-001.png": "../outside.png"})

    def test_cohort_summary_omits_cohorts_not_evaluated(self) -> None:
        predictions = [
            {
                "actual": "hair_salon",
                "predicted": "hair_salon",
                "entityCohort": "warm",
            }
        ]
        summaries = _cohort_classification_summaries(predictions, ["hair_salon"])
        self.assertEqual(["warm"], list(summaries))
        self.assertEqual(1.0, summaries["warm"]["macroRecall"])

    def test_holdout_definition_rejects_incomplete_category_contract(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            definition = {
                "holdoutVersion": "fixture-v1",
                "imagesDirectory": "images",
                "generation": {"promptVersion": "fixture-v1"},
                "categories": {"peluqueria": []},
            }
            assets, report = inspect_holdout_assets(Path(temporary), definition)
            self.assertEqual([], assets)
            self.assertFalse(report["passed"])
            self.assertIn(
                "HOLDOUT_CATEGORY_COVERAGE_INVALID",
                {item["code"] for item in report["violations"]},
            )
            self.assertIn(
                "HOLDOUT_CATEGORY_BALANCE_INVALID",
                {item["code"] for item in report["violations"]},
            )

    def test_versioned_real_report_remains_blocked_after_failed_quality_gate(self) -> None:
        dataset = ROOT / "evaluation" / "synthetic-marketplace-v1"
        assets = [json.loads(line) for line in (dataset / "image-assets.jsonl").read_text(encoding="utf-8").splitlines()]
        report = json.loads((dataset / "visual-qa-report.json").read_text(encoding="utf-8"))
        self.assertEqual(100, len(assets))
        self.assertTrue(report["structural"]["passed"])
        self.assertFalse(report["clipCategoryDiagnostic"]["categoryQualityPassed"])
        self.assertFalse(report["automatedQualityPassed"])
        self.assertFalse(report["overallPassed"])
        self.assertFalse(report["humanReviewCompleted"])
        self.assertTrue(all(not asset["developmentTrainingAllowed"] for asset in assets))
        self.assertTrue(all(not asset["productionTrainingAllowed"] for asset in assets))


if __name__ == "__main__":
    unittest.main()
