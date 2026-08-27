"""Pruebas estructurales de la QA visual sintética."""

from __future__ import annotations

import json
import random
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

from reserly_demand_engine.synthetic_visual_qa import CATEGORY_PROMPTS, inspect_assets

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
