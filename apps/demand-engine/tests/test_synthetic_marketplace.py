"""Pruebas del dataset sintético versionado para recomendación offline."""

from __future__ import annotations

import hashlib
import json
import tempfile
import unittest
from collections import Counter
from datetime import datetime
from pathlib import Path

from reserly_demand_engine.synthetic_marketplace import generate_dataset


def _rows(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


class SyntheticMarketplaceTests(unittest.TestCase):
    """Verifica cardinalidad, reproducibilidad, privacidad y ausencia de leakage."""

    def test_generates_expected_counts_and_non_perfect_noisy_labels(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            result = generate_dataset(root)
            manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
            sessions = _rows(root / "ranking-sessions.jsonl")
            self.assertEqual(result.venue_count, 100)
            self.assertEqual(result.profile_count, 40)
            self.assertEqual(result.session_count, 2400)
            self.assertEqual(result.candidate_count, 19_200)
            self.assertEqual(manifest["counts"]["imagePrompts"], 100)
            expected_categories = {
                "restaurante", "peluqueria", "campo-de-futbol", "pista-de-padel",
                "instalacion-municipal", "centro-deportivo", "centro-de-estetica", "otros",
            }
            self.assertEqual(set(manifest["categoryCoverage"]), expected_categories)
            for coverage in manifest["categoryCoverage"].values():
                self.assertGreater(coverage["warm"], 0)
                self.assertGreater(coverage["validationCold"], 0)
                self.assertGreater(coverage["testCold"], 0)
            labels = [candidate["labels"] for session in sessions for candidate in session["candidates"]]
            conversion_rate = sum(label["bookingCompleted"] for label in labels) / len(sessions)
            click_rate = sum(label["clicked"] for label in labels) / len(sessions)
            self.assertGreater(conversion_rate, 0.05)
            self.assertLess(conversion_rate, 0.65)
            self.assertGreater(click_rate, conversion_rate)
            self.assertLess(click_rate, 0.90)

    def test_is_byte_reproducible_for_same_seed(self) -> None:
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            generate_dataset(Path(first), seed=1729)
            generate_dataset(Path(second), seed=1729)
            for name in ("venues.jsonl", "profiles.jsonl", "ranking-sessions.jsonl", "image-prompts.jsonl", "manifest.json"):
                left = hashlib.sha256((Path(first) / name).read_bytes()).hexdigest()
                right = hashlib.sha256((Path(second) / name).read_bytes()).hexdigest()
                self.assertEqual(left, right, name)

    def test_temporal_and_entity_cohorts_prevent_leakage(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            generate_dataset(root)
            sessions = _rows(root / "ranking-sessions.jsonl")
            venue_cohorts = {row["venueId"]: row["entityCohort"] for row in _rows(root / "venues.jsonl")}
            split_counts = Counter(session["split"] for session in sessions)
            self.assertEqual(split_counts, {"train": 1400, "validation": 400, "test": 600})
            ranges = {
                "train": (datetime.fromisoformat("2026-01-01T00:00:00+00:00"), datetime.fromisoformat("2026-04-30T23:59:59+00:00")),
                "validation": (datetime.fromisoformat("2026-05-01T00:00:00+00:00"), datetime.fromisoformat("2026-05-31T23:59:59+00:00")),
                "test": (datetime.fromisoformat("2026-06-01T00:00:00+00:00"), datetime.fromisoformat("2026-06-30T23:59:59+00:00")),
            }
            for session in sessions:
                occurred = datetime.fromisoformat(session["occurredAt"].replace("Z", "+00:00"))
                self.assertGreaterEqual(occurred, ranges[session["split"]][0])
                self.assertLessEqual(occurred, ranges[session["split"]][1])
                cohorts = {venue_cohorts[candidate["venueId"]] for candidate in session["candidates"]}
                if session["split"] == "train":
                    self.assertEqual(cohorts, {"warm"})
                    self.assertEqual(session["profileCohort"], "warm")
                elif session["split"] == "validation":
                    self.assertNotIn("testCold", cohorts)
                    self.assertNotEqual(session["profileCohort"], "testCold")

    def test_privacy_and_visual_training_are_fail_closed(self) -> None:
        forbidden = {"email", "phone", "gender", "age", "health", "payment", "address", "postcode"}
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            generate_dataset(root)
            profiles = _rows(root / "profiles.jsonl")
            prompts = _rows(root / "image-prompts.jsonl")
            serialized = json.dumps(profiles, ensure_ascii=False).lower()
            for fragment in forbidden:
                self.assertNotIn(f'"{fragment}"', serialized)
            self.assertTrue(all(not row["privacy"]["containsDirectIdentifiers"] for row in profiles))
            self.assertTrue(all(not prompt["materialized"] for prompt in prompts))
            self.assertTrue(all(not prompt["trainingAllowed"] for prompt in prompts))
            self.assertEqual(len({prompt["prompt"] for prompt in prompts}), 100)


if __name__ == "__main__":
    unittest.main()
