"""Regresión del candidato multirregión robusto v4 development-only."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_multiregion_v4 import freeze_candidate


REPO = Path(__file__).resolve().parents[3]
DATASET = REPO / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
RESULTS = REPO / "apps/demand-engine/evaluation/results"
MODELS = REPO / "apps/demand-engine/models"
POLICIES = REPO / "apps/demand-engine/policies"
EMBEDDINGS = DATASET / "development-multiregion-embeddings.v4.json"
PRELIMINARY = RESULTS / "full-taxonomy-visual-multiregion-development.v4.json"
REPORT = RESULTS / "full-taxonomy-visual-multiregion-robust-development.v4.json"
MODEL = MODELS / "full-taxonomy-visual-multiregion-classifier.v4.json"
POLICY = POLICIES / "full-taxonomy-visual-multiregion-holdout.v4.json"


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def test_consumed_four_view_corpus_has_two_pixel_regions_and_no_new_images() -> None:
    artifact = json.loads(EMBEDDINGS.read_text(encoding="utf-8"))
    assert artifact["split"] == "consumed-development-only"
    assert artifact["imageCount"] == len(artifact["rows"]) == 1016
    assert artifact["viewCounts"] == {"A": 254, "B": 254, "C": 254, "D": 254}
    assert artifact["regions"] == ["global", "center-80-percent"]
    assert artifact["newImagesGenerated"] == 0
    assert artifact["independentTestAvailable"] is False
    assert len({row["imageId"] for row in artifact["rows"]}) == 1016
    assert all(len(row["globalEmbedding"]) == len(row["centerEmbedding"]) == 512 for row in artifact["rows"])


def test_robust_selection_improves_mean_and_consumed_d_without_hiding_worst_fold() -> None:
    report = json.loads(REPORT.read_text(encoding="utf-8"))
    metrics = report["selectedDevelopmentMetrics"]
    assert report["selectedCandidate"] == "global-center-prototype-lda-robust-0.75"
    assert metrics == {"accuracy": 0.83267717, "error": 0.16732283,
                       "macroPrecision": 0.85670008, "macroRecall": 0.81304345,
                       "macroF1": 0.81591205, "recallAt3": 0.96062992,
                       "minimumClassRecall": 0.375}
    assert report["developmentAccuracyUplift"] == pytest.approx(.03346457)
    assert report["selectedWorstFoldAccuracy"] == pytest.approx(.78346457)
    assert report["selectedWorstFoldMacroF1"] == pytest.approx(.76688298)
    assert report["consumedViewDMetrics"]["accuracy"] == pytest.approx(.78346457)
    assert report["viewDAccuracyUplift"] == pytest.approx(.03543307)
    assert report["freshHoldoutRequired"] is True
    assert report["qualityConfirmed"] is False


def test_frozen_model_is_pixel_only_portable_and_not_promotable() -> None:
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    report = json.loads(REPORT.read_text(encoding="utf-8"))
    assert model["inputFeatures"] == ["clipGlobalEmbedding512", "clipCenter80Embedding512"]
    assert set(model["prohibitedInputFeatures"]) == {"prompt", "typeCode", "familyCode", "archetypeCode"}
    assert len(model["globalPrototypes"]) == len(model["centerPrototypes"]) == 254
    assert len(model["ldaWeights"]) == len(model["classes"]) == 23
    assert model["fusionAlpha"] == pytest.approx(.75)
    assert model["independentTestEvaluated"] is False
    assert model["productionTrainingAllowed"] is False and model["promotionAllowed"] is False
    assert policy["freshHoldoutRequired"] is True
    assert policy["automaticPromotionAllowed"] is False
    assert report["modelSha256"] == _sha(MODEL)
    assert report["policySha256"] == _sha(POLICY)


def test_second_candidate_freeze_is_rejected() -> None:
    with pytest.raises(ValueError, match="CANDIDATE_ALREADY_FROZEN"):
        freeze_candidate(EMBEDDINGS, PRELIMINARY, REPORT, MODEL, POLICY)
