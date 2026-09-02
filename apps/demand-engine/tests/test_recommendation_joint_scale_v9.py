from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pytest

from reserly_demand_engine.recommendation_joint_scale_dataset import (
    ACTION_TYPES, CANDIDATES_PER_SESSION, FEATURE_NAMES, FLAG_NAMES,
)
from reserly_demand_engine.recommendation_joint_scale_training import open_test


ROOT = Path(__file__).resolve().parents[3]
ENGINE = ROOT / "apps/demand-engine"
DATASET = ENGINE / "evaluation/synthetic-marketplace-joint-scale-v9"
RESULT = ENGINE / "evaluation/results/recommendation-joint-scale.v10.json"


def test_scaled_dataset_covers_thousands_and_full_taxonomy() -> None:
    manifest = json.loads((DATASET / "manifest.json").read_text(encoding="utf-8"))
    assert manifest["counts"]["users"] >= 2000
    assert manifest["counts"]["venues"] >= 2000
    assert manifest["counts"]["taxonomyTypes"] == 254
    assert manifest["counts"]["taxonomyFamilies"] == 23
    assert manifest["counts"]["candidateRows"] == 288000
    assert manifest["counts"]["actionTypes"] == len(ACTION_TYPES) == 10
    assert manifest["visualPolicy"]["uniqueImageVenueMappings"] == 1016
    assert manifest["visualPolicy"]["missingVisualEvidenceUsesZeroSignalFallback"] is True
    profiles = [json.loads(line) for line in (DATASET / "profiles.jsonl").read_text(encoding="utf-8").splitlines()]
    venues = [json.loads(line) for line in (DATASET / "venues.jsonl").read_text(encoding="utf-8").splitlines()]
    assert len({row["profileId"] for row in profiles}) == 2500
    assert len({row["venueId"] for row in venues}) == 3000
    assert len({row["typeCode"] for row in venues}) == 254
    assert len({row["familyCode"] for row in venues}) == 23
    image_ids = [row["visualEvidence"]["imageId"] for row in venues if row["visualEvidence"]]
    assert len(image_ids) == len(set(image_ids)) == 1016


def test_scaled_arrays_have_expected_contract_and_diverse_slices() -> None:
    with np.load(DATASET / "development.npz", allow_pickle=False) as data:
        assert data["features"].shape == (18000, CANDIDATES_PER_SESSION, len(FEATURE_NAMES))
        assert data["positiveIndices"].shape == (18000,)
        assert set(np.unique(data["actionCodes"])) >= set(range(len(ACTION_TYPES)))
        assert np.all(data["scenarioFlags"].sum(axis=0) > 0)
        assert data["scenarioFlags"].shape[1] == len(FLAG_NAMES)
        assert np.isfinite(data["features"]).all()
        assert np.all((data["positiveIndices"] >= 0) & (data["positiveIndices"] < CANDIDATES_PER_SESSION))


def test_joint_result_passes_offline_gates_without_production_promotion() -> None:
    result = json.loads(RESULT.read_text(encoding="utf-8"))
    assert result["counts"]["users"] == 2500
    assert result["counts"]["venues"] == 3000
    assert result["jointTrainingMetrics"]["accuracy"] <= 0.90
    assert result["jointTestMetrics"]["accuracy"] >= 0.90
    assert result["jointTestMetrics"]["errorRate"] < 0.15
    assert result["visualAccuracyUplift"] >= 0.03
    assert result["qualityGatesPassed"] is True
    assert result["singleScoringModel"] is True
    assert result["productionEvidence"] is False
    assert result["promotionAllowed"] is False


def test_test_budget_is_consumed_and_reopening_fails(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="TEST_ALREADY_OPENED"):
        open_test(
            DATASET,
            ENGINE / "policies/recommendation-joint-scale.v10.json",
            ENGINE / "evaluation/results/recommendation-joint-scale-development.v10.json",
            ENGINE / "models/contextual-scale-ablation.v10.linear.json",
            ENGINE / "models/joint-context-visual-ranker.v10.linear.json",
            DATASET / "pretest-lock.v10.json",
            RESULT,
            DATASET / "test-opening-record.v10.json",
        )
