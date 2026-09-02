"""Regresión de selección development-only y apertura única v2."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_training_v2 import open_test


REPO_ROOT = Path(__file__).resolve().parents[3]
DATASET = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"
RESULTS = REPO_ROOT / "apps/demand-engine/evaluation/results"
MODELS = REPO_ROOT / "apps/demand-engine/models"
POLICIES = REPO_ROOT / "apps/demand-engine/policies"


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def test_development_selects_without_reading_holdout() -> None:
    report = json.loads((RESULTS / "full-taxonomy-visual-development.v2.json").read_text(encoding="utf-8"))
    lock = json.loads((DATASET / "pretest-lock.v2.json").read_text(encoding="utf-8"))

    assert report["imageCount"] == report["typeCount"] == 254
    assert report["familyCount"] == 23
    assert report["foldCount"] == 4
    assert len(report["candidates"]) == 9
    assert report["selectedCandidate"] == "centroid"
    assert report["holdoutEmbeddingsRead"] is False
    assert report["holdoutPredictionsComputed"] is False
    assert lock["holdoutImageCount"] == 254
    assert lock["holdoutPredictionsComputedBeforeLock"] is False
    assert lock["budget"] == 1 and lock["consumed"] == 0
    assert lock["modelSha256"] == _sha(MODELS / "full-taxonomy-visual-family-classifier.v2.json")


def test_holdout_result_is_preserved_even_though_top1_fails() -> None:
    result = json.loads((RESULTS / "full-taxonomy-visual-holdout.v2.json").read_text(encoding="utf-8"))
    opening = json.loads((DATASET / "test-opening-record.v2.json").read_text(encoding="utf-8"))

    assert result["opening"] == "1/1"
    assert result["holdoutImageCount"] == result["typeCount"] == 254
    assert result["holdoutMetrics"]["accuracy"] == 0.70472441
    assert result["holdoutMetrics"]["error"] == 0.29527559
    assert result["holdoutMetrics"]["familyRecallAt3"] == 0.90551181
    assert result["qualityGatesPassed"] is False
    assert result["qualityGates"] == {
        "accuracyPassed": False,
        "errorPassed": False,
        "macroPrecisionPassed": False,
        "macroRecallPassed": False,
        "macroF1Passed": False,
        "generalizationGapPassed": True,
    }
    assert result["productionEvidence"] is False
    assert result["promotionAllowed"] is False
    assert opening["budget"] == opening["consumed"] == 1
    assert opening["reopenAllowed"] is False
    assert opening["resultSha256"] == _sha(RESULTS / "full-taxonomy-visual-holdout.v2.json")


def test_development_and_holdout_embeddings_are_disjoint() -> None:
    development = json.loads((DATASET / "development-clip-embeddings.v2.json").read_text(encoding="utf-8"))
    holdout = json.loads((DATASET / "holdout-clip-embeddings.v2.json").read_text(encoding="utf-8"))

    assert len(development["rows"]) == len(holdout["rows"]) == 254
    assert {row["imageId"] for row in development["rows"]}.isdisjoint(row["imageId"] for row in holdout["rows"])
    assert {row["venueId"] for row in development["rows"]}.isdisjoint(row["venueId"] for row in holdout["rows"])
    assert {row["imageSha256"] for row in development["rows"]}.isdisjoint(row["imageSha256"] for row in holdout["rows"])


def test_second_holdout_opening_fails_before_inference() -> None:
    with pytest.raises(ValueError, match="HOLDOUT_ALREADY_OPENED"):
        open_test(
            manifest_path=DATASET / "generation-manifest.v2.json",
            authorization_path=DATASET / "human-review-authorization.v2.json",
            model_manifest_path=MODELS / "clip-vit-b32-visual-evidence.v1.json",
            development_embeddings_path=DATASET / "development-clip-embeddings.v2.json",
            development_report_path=RESULTS / "full-taxonomy-visual-development.v2.json",
            model_path=MODELS / "full-taxonomy-visual-family-classifier.v2.json",
            policy_path=POLICIES / "full-taxonomy-visual-holdout.v2.json",
            lock_path=DATASET / "pretest-lock.v2.json",
            holdout_embeddings_path=DATASET / "holdout-clip-embeddings.v2.json",
            result_path=RESULTS / "full-taxonomy-visual-holdout.v2.json",
            opening_record_path=DATASET / "test-opening-record.v2.json",
        )
