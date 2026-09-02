"""Regresión del recomendador por acciones, ubicación y escasez v6."""

from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path

import pytest

from reserly_demand_engine.recommendation_action_context_dataset import FEATURE_NAMES, _haversine_km
from reserly_demand_engine.recommendation_action_context_training import behavioral_scenarios, open_test


ROOT = Path(__file__).parents[1]
DATASET = ROOT / "evaluation/synthetic-marketplace-action-context-v6"
POLICY = ROOT / "policies/recommendation-action-context.v6.json"
DEVELOPMENT = ROOT / "evaluation/results/recommendation-action-context-development.v6.json"
RESULT = ROOT / "evaluation/results/recommendation-action-context.v6.json"
MODEL = ROOT / "models/contextual-recommender-action-context.v6.xgb.json"


def _rows(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def test_dataset_covers_profiles_venues_actions_and_temporal_holdout() -> None:
    manifest = json.loads((DATASET / "manifest.json").read_text(encoding="utf-8"))
    development = _rows(DATASET / "development-sessions.jsonl")
    test = _rows(DATASET / "test-sessions.sealed.jsonl")

    assert manifest["counts"] == {"venues": 100, "profiles": 40, "sessions": 3200,
                                   "candidates": 25600, "actionEvents": 17596,
                                   "actionTypes": 10, "families": 6, "types": 28}
    assert manifest["relevancePolicy"] == "adjudicatedPointInTimeUtilityThenObservedChoiceNoise"
    assert len(development) == 2400 and len(test) == 800
    assert max(datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00")) for row in development) < min(
        datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00")) for row in test
    )


def test_location_is_point_in_time_haversine_and_not_raw_model_input() -> None:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    manifest = json.loads((DATASET / "manifest.json").read_text(encoding="utf-8"))

    assert _haversine_km((40.4168, -3.7038), (41.3874, 2.1686)) == pytest.approx(505.1, abs=2)
    assert manifest["locationPolicy"]["distanceMethod"] == "haversine"
    assert manifest["locationPolicy"]["rawCoordinatesAsFeatures"] is False
    assert policy["locationPolicy"]["required"] is True
    assert {"latitude", "longitude", "venueId", "profileId", "position"}.isdisjoint(policy["featureNames"])
    assert {"currentLocationProximity", "withinPreferredRadius", "distanceDecayKm"}.issubset(policy["featureNames"])


def test_scarcity_is_only_an_intent_location_and_capacity_interaction() -> None:
    manifest = json.loads((DATASET / "manifest.json").read_text(encoding="utf-8"))
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    assert manifest["scarcityPolicy"]["globalScarcityBoostForbidden"] is True
    assert "contentAffinity*currentLocationProximity*withinPreferredRadius" in manifest["scarcityPolicy"]["formula"]
    assert policy["scarcityPolicy"] == {"globalBoostForbidden": True, "requiresIntentAffinity": True,
                                         "requiresPositiveCapacity": True, "requiresLocationFit": True}
    assert all(candidate["availability"]["remainingSlots"] > 0
               for session in _rows(DATASET / "test-sessions.sealed.jsonl") for candidate in session["candidates"])


def test_action_feature_contract_is_pre_ranking_and_behavioral_scenarios_pass() -> None:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    result = json.loads(RESULT.read_text(encoding="utf-8"))
    forbidden = {"clicked", "bookingCompleted", "relevance", "outcomeObservedAt"}
    assert policy["featureNames"] == FEATURE_NAMES
    assert forbidden.isdisjoint(policy["featureNames"])
    assert len(behavioral_scenarios(policy["featureNames"])) == 10
    assert result["behavioralScenarioMetrics"]["passed"] == 10


def test_five_fold_training_and_fresh_test_pass_quality_and_slice_gates() -> None:
    development = json.loads(DEVELOPMENT.read_text(encoding="utf-8"))
    result = json.loads(RESULT.read_text(encoding="utf-8"))
    assert development["folds"] == 5
    assert development["trainingMetrics"]["accuracy"] == pytest.approx(.8835)
    assert development["trainingMetrics"]["accuracy"] <= .90
    assert result["testMetrics"]["accuracy"] == pytest.approx(.9125)
    assert result["testMetrics"]["errorRate"] == pytest.approx(.0875)
    assert result["testMetrics"]["precision"] >= .80
    assert result["testMetrics"]["recall"] >= .80
    assert result["testMetrics"]["f1"] >= .80
    assert result["trainTestAccuracyGap"] == pytest.approx(.029)
    assert result["testSliceMetrics"]["scarceAligned"]["accuracy"] >= .99
    assert result["testSliceMetrics"]["locationSensitive"]["accuracy"] >= .90
    assert result["testSliceMetrics"]["intentPivot"]["accuracy"] >= .90
    assert result["qualityGatesPassed"] is True
    assert result["productionEvidence"] is False and result["promotionAllowed"] is False


def test_consumed_holdout_cannot_be_opened_again(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="RECOMMENDATION_ACTION_CONTEXT_TEST_ALREADY_OPENED"):
        open_test(DATASET, POLICY, DEVELOPMENT, MODEL, DATASET / "pretest-lock.v6.json",
                  tmp_path / "result.json", DATASET / "test-opening-record.v6.json")
