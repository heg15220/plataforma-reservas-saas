"""Regresión final del recomendador contextual v8 con tiempo discriminativo."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from reserly_demand_engine.recommendation_action_context_training import open_test


ROOT = Path(__file__).parents[1]
DATASET = ROOT / "evaluation/synthetic-marketplace-action-context-v8"
POLICY = ROOT / "policies/recommendation-action-context.v8.json"
DEVELOPMENT = ROOT / "evaluation/results/recommendation-action-context-development.v8.json"
RESULT = ROOT / "evaluation/results/recommendation-action-context.v8.json"
MODEL = ROOT / "models/contextual-recommender-action-context.v8.xgb.json"


def _rows(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def test_day_and_hour_affinity_are_candidate_specific_not_constant_features() -> None:
    manifest = json.loads((DATASET / "manifest.json").read_text(encoding="utf-8"))
    sessions = _rows(DATASET / "development-sessions.jsonl")
    assert manifest["schemaVersion"] == 8
    assert manifest["temporalAffinityPolicy"] == "candidateSpecificDayAndHour"
    assert sum(len({row["features"]["requestedDayAffinity"] for row in session["candidates"]}) > 1 for session in sessions) >= 2390
    assert sum(len({row["features"]["requestedHourAffinity"] for row in session["candidates"]}) > 1 for session in sessions) >= 2390


def test_v8_passes_training_test_and_behavioral_gates() -> None:
    development = json.loads(DEVELOPMENT.read_text(encoding="utf-8"))
    result = json.loads(RESULT.read_text(encoding="utf-8"))
    assert development["folds"] == 5
    assert development["trainingMetrics"]["accuracy"] == pytest.approx(.8735)
    assert development["trainingMetrics"]["accuracy"] <= .90
    assert result["testMetrics"]["accuracy"] == pytest.approx(.90625)
    assert result["testMetrics"]["errorRate"] == pytest.approx(.09375)
    assert result["testMetrics"]["precision"] == pytest.approx(.90625)
    assert result["testMetrics"]["recall"] == pytest.approx(.90625)
    assert result["testMetrics"]["f1"] == pytest.approx(.90625)
    assert result["trainTestAccuracyGap"] == pytest.approx(.03275)
    assert result["behavioralScenarioMetrics"]["passed"] == 10
    assert result["qualityGatesPassed"] is True
    assert result["productionEvidence"] is False and result["promotionAllowed"] is False


def test_location_scarcity_and_time_slices_are_strong() -> None:
    slices = json.loads(RESULT.read_text(encoding="utf-8"))["testSliceMetrics"]
    assert slices["scarceAligned"]["accuracy"] >= .99
    assert slices["locationSensitive"]["accuracy"] >= .92
    assert slices["evening"]["accuracy"] >= .90
    assert slices["intentPivot"]["accuracy"] >= .80
    assert slices["coldVenue"]["accuracy"] >= .80


def test_v8_holdout_budget_is_consumed_and_cannot_be_reopened(tmp_path: Path) -> None:
    record = DATASET / "test-opening-record.v8.json"
    assert json.loads(record.read_text(encoding="utf-8"))["testOpenCount"] == 1
    with pytest.raises(ValueError, match="RECOMMENDATION_ACTION_CONTEXT_TEST_ALREADY_OPENED"):
        open_test(DATASET, POLICY, DEVELOPMENT, MODEL, DATASET / "pretest-lock.v8.json",
                  tmp_path / "result.json", record)
