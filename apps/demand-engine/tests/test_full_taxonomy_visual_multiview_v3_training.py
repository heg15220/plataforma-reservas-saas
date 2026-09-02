"""Regresión del desarrollo multivista v3 sin apertura del holdout."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_multiview_v3_training import develop


REPO_ROOT = Path(__file__).resolve().parents[3]
DATASET = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
V2 = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"
RESULTS = REPO_ROOT / "apps/demand-engine/evaluation/results"
MODELS = REPO_ROOT / "apps/demand-engine/models"
POLICIES = REPO_ROOT / "apps/demand-engine/policies"


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def test_multiview_development_used_three_views_without_reading_holdout_during_selection() -> None:
    embeddings = json.loads(
        (DATASET / "development-clip-embeddings.v3.json").read_text(encoding="utf-8")
    )
    report = json.loads(
        (RESULTS / "full-taxonomy-visual-multiview-development.v3.json").read_text(
            encoding="utf-8"
        )
    )

    assert embeddings["imageCount"] == len(embeddings["rows"]) == 762
    assert embeddings["viewCounts"] == {"A": 254, "B": 254, "C": 254}
    assert embeddings["provenance"]["reusedEmbeddingCount"] == 508
    assert embeddings["provenance"]["newlyExtractedEmbeddingCount"] == 254
    assert embeddings["provenance"]["holdoutV3EmbeddingCount"] == 0
    assert {row["developmentView"] for row in embeddings["rows"]} == {"A", "B", "C"}
    assert len({row["imageId"] for row in embeddings["rows"]}) == 762
    assert report["imageCount"] == 762
    assert report["typeCount"] == 254
    assert report["familyCount"] == 23
    assert report["archetypeCount"] == 38
    assert report["foldCount"] == 3
    assert len(report["familyCandidates"]) == 35
    assert len(report["archetypeCandidates"]) == 31
    assert report["holdoutV3ImagesRead"] is False
    assert report["holdoutV3EmbeddingsRead"] is False
    assert report["holdoutV3PredictionsComputed"] is False
    # El informe development queda como evidencia histórica de selección limpia;
    # tras 23.22.e el artefacto holdout existe legítimamente y está consumido.
    assert (DATASET / "holdout-clip-embeddings.v3.json").exists()


def test_selected_heads_and_development_metrics_are_preserved() -> None:
    report = json.loads(
        (RESULTS / "full-taxonomy-visual-multiview-development.v3.json").read_text(
            encoding="utf-8"
        )
    )
    model = json.loads(
        (MODELS / "full-taxonomy-visual-multiview-classifier.v3.json").read_text(
            encoding="utf-8"
        )
    )

    assert report["selectedFamilyCandidate"] == "type-prototype-archetype-fusion-0.25"
    assert report["selectedFamilyDevelopmentMetrics"] == {
        "accuracy": 0.79527559,
        "error": 0.20472441,
        "macroPrecision": 0.81518665,
        "macroRecall": 0.76498406,
        "macroF1": 0.76972758,
        "recallAt3": 0.95669291,
        "minimumClassRecall": 0.36111111,
    }
    assert report["selectedArchetypeCandidate"] == "lda-1"
    assert report["selectedArchetypeDevelopmentMetrics"]["macroF1"] == 0.71199779
    assert model["inputFeatures"] == ["clipImageEmbedding512"]
    assert set(model["prohibitedInputFeatures"]) == {
        "prompt",
        "typeCode",
        "familyCode",
        "archetypeCode",
    }
    assert model["familyHead"]["kind"] == "archetypeFusion"
    assert model["archetypeHead"]["kind"] == "lda"
    assert model["productionTrainingAllowed"] is False
    assert model["promotionAllowed"] is False


def test_pretest_lock_still_binds_frozen_artifacts_after_budget_consumption() -> None:
    lock = json.loads((DATASET / "pretest-lock.v3.json").read_text(encoding="utf-8"))

    assert lock["manifestSha256"] == _sha(DATASET / "generation-manifest.v3.json")
    assert lock["authorizationSha256"] == _sha(
        DATASET / "human-review-authorization.v3.json"
    )
    assert lock["developmentEmbeddingsSha256"] == _sha(
        DATASET / "development-clip-embeddings.v3.json"
    )
    assert lock["developmentReportSha256"] == _sha(
        RESULTS / "full-taxonomy-visual-multiview-development.v3.json"
    )
    assert lock["modelSha256"] == _sha(
        MODELS / "full-taxonomy-visual-multiview-classifier.v3.json"
    )
    assert lock["policySha256"] == _sha(
        POLICIES / "full-taxonomy-visual-multiview-holdout.v3.json"
    )
    assert lock["holdoutImageCount"] == 254
    assert lock["holdoutEmbeddingsCreatedBeforeLock"] is False
    assert lock["holdoutPredictionsComputedBeforeLock"] is False
    assert lock["budget"] == 1
    assert lock["consumed"] == 1
    assert lock["status"] == "consumed"
    assert lock["qualityGatesPassed"] is False
    assert lock["reopenAllowed"] is False


def test_second_development_freeze_fails_before_any_inference() -> None:
    with pytest.raises(ValueError, match="DEVELOPMENT_ALREADY_FROZEN"):
        develop(
            manifest_path=DATASET / "generation-manifest.v3.json",
            authorization_path=DATASET / "human-review-authorization.v3.json",
            model_manifest_path=MODELS / "clip-vit-b32-visual-evidence.v1.json",
            reused_a_path=V2 / "development-clip-embeddings.v2.json",
            reused_b_path=V2 / "holdout-clip-embeddings.v2.json",
            embeddings_path=DATASET / "development-clip-embeddings.v3.json",
            report_path=RESULTS / "full-taxonomy-visual-multiview-development.v3.json",
            model_path=MODELS / "full-taxonomy-visual-multiview-classifier.v3.json",
            policy_path=POLICIES / "full-taxonomy-visual-multiview-holdout.v3.json",
            lock_path=DATASET / "pretest-lock.v3.json",
            holdout_embeddings_path=DATASET / "holdout-clip-embeddings.v3.json",
        )
