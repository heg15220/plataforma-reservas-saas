"""Regresión de la apertura única del holdout visual multivista v3."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_multiview_v3_holdout import open_holdout


REPO = Path(__file__).resolve().parents[3]
DATASET = REPO / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
RESULTS = REPO / "apps/demand-engine/evaluation/results"
MODELS = REPO / "apps/demand-engine/models"
POLICIES = REPO / "apps/demand-engine/policies"


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def test_holdout_embeddings_cover_exactly_the_authorized_v3_rows() -> None:
    manifest = json.loads((DATASET / "generation-manifest.v3.json").read_text(encoding="utf-8"))
    artifact = json.loads((DATASET / "holdout-clip-embeddings.v3.json").read_text(encoding="utf-8"))
    expected = {row["imageId"]: row for row in manifest["holdoutRows"]}

    assert artifact["split"] == "sealedHoldoutV3-consumed"
    assert artifact["imageCount"] == len(artifact["rows"]) == 254
    assert artifact["dimensions"] == 512
    assert len({row["imageId"] for row in artifact["rows"]}) == 254
    assert len({row["venueId"] for row in artifact["rows"]}) == 254
    assert all(len(row["embedding"]) == 512 for row in artifact["rows"])
    assert all(row["imageSha256"] == expected[row["imageId"]]["generation"]["imageSha256"] for row in artifact["rows"])
    assert artifact["productionTrainingAllowed"] is False
    assert artifact["promotionAllowed"] is False


def test_real_holdout_metrics_are_preserved_even_though_gates_fail() -> None:
    result = json.loads((RESULTS / "full-taxonomy-visual-multiview-holdout.v3.json").read_text(encoding="utf-8"))
    assert result["imageCount"] == result["typeCount"] == 254
    assert result["familyCount"] == 23 and result["archetypeCount"] == 38
    assert result["familyMetrics"] == {
        "accuracy": 0.7480315, "error": 0.2519685,
        "macroPrecision": 0.79612678, "macroRecall": 0.71791241,
        "macroF1": 0.72358713, "recallAt3": 0.92913386,
        "minimumClassRecall": 0.25,
        "perClassRecall": result["familyMetrics"]["perClassRecall"],
    }
    assert result["generalizationAccuracyGap"] == pytest.approx(.04724409)
    assert result["gateResults"] == {"accuracy": False, "error": False, "generalizationGap": True,
                                     "macroF1": False, "macroPrecision": False, "macroRecall": False,
                                     "minimumClassRecall": False}
    assert result["qualityGatesPassed"] is False
    assert result["trainingAllowed"] is False
    assert result["promotionAllowed"] is False


def test_budget_hashes_and_opening_record_are_consumed_irreversibly() -> None:
    lock = json.loads((DATASET / "pretest-lock.v3.json").read_text(encoding="utf-8"))
    record = json.loads((DATASET / "holdout-opening-record.v3.json").read_text(encoding="utf-8"))
    embeddings = DATASET / "holdout-clip-embeddings.v3.json"
    result = RESULTS / "full-taxonomy-visual-multiview-holdout.v3.json"

    assert lock["budget"] == lock["consumed"] == 1
    assert lock["status"] == "consumed" and lock["reopenAllowed"] is False
    assert lock["holdoutEmbeddingsSha256"] == record["holdoutEmbeddingsSha256"] == _sha(embeddings)
    assert lock["holdoutResultSha256"] == record["resultSha256"] == _sha(result)
    assert lock["openingRecordSha256"] == _sha(DATASET / "holdout-opening-record.v3.json")
    assert record["selectionUsedHoldout"] is False
    assert record["familyQualityGatesPassed"] is False


def test_second_holdout_open_fails_before_loading_clip(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="HOLDOUT_ALREADY_OPENED"):
        open_holdout(
            DATASET / "generation-manifest.v3.json", DATASET / "human-review-authorization.v3.json",
            MODELS / "clip-vit-b32-visual-evidence.v1.json", DATASET / "development-clip-embeddings.v3.json",
            RESULTS / "full-taxonomy-visual-multiview-development.v3.json",
            MODELS / "full-taxonomy-visual-multiview-classifier.v3.json",
            POLICIES / "full-taxonomy-visual-multiview-holdout.v3.json", DATASET / "pretest-lock.v3.json",
            DATASET / "holdout-clip-embeddings.v3.json", tmp_path / "result.json",
            DATASET / "holdout-opening-record.v3.json",
        )
