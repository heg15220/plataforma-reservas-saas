"""Regresión del dataset y recomendador contextual diverso v2."""

from __future__ import annotations

import hashlib
import json
from datetime import datetime
from pathlib import Path

import pytest

from reserly_demand_engine.recommendation_diverse_dataset import generate_diverse_dataset
from reserly_demand_engine.recommendation_diverse_training import _rolling_folds, business_scenarios, open_test


ROOT = Path(__file__).parents[1]
DATASET = ROOT / "evaluation/synthetic-marketplace-diverse-v2"
SOURCE = ROOT / "evaluation/synthetic-marketplace-v1"
TAXONOMY = ROOT.parents[1] / "packages/demand-contracts/catalog/venue-taxonomy.v1.json"
POLICY = ROOT / "policies/recommendation-cross-validation-diverse.v2.json"
DEVELOPMENT_REPORT = ROOT / "evaluation/results/recommendation-diverse-development.v2.json"
RESULT = ROOT / "evaluation/results/recommendation-cross-validation-diverse.v2.json"
MODEL = ROOT / "models/contextual-recommender-diverse-cv.v2.xgb.json"


def _rows(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def test_dataset_is_reproducible_diverse_and_does_not_generate_images(tmp_path: Path) -> None:
    generated = tmp_path / "generated"
    manifest = generate_diverse_dataset(SOURCE, TAXONOMY, generated)

    assert manifest["counts"] == {
        "venues": 100, "profiles": 40, "sessions": 2700, "candidates": 21600,
        "families": 6, "types": 28,
    }
    assert manifest["visualPolicy"] == {
        "newImagesGenerated": 0,
        "rawPixelsUsedForRecommendationTraining": False,
        "allowedSignal": "declaredAmbienceMetadataOnly",
    }
    assert manifest["taxonomyActivationStatus"] == "candidateOnly"
    for name in ("venue-labels.jsonl", "development-sessions.jsonl", "test-sessions.sealed.jsonl"):
        assert hashlib.sha256((generated / name).read_bytes()).hexdigest() == hashlib.sha256(
            (DATASET / name).read_bytes()
        ).hexdigest()


def test_development_and_test_are_temporally_separate_with_declared_ambiguity() -> None:
    development = _rows(DATASET / "development-sessions.jsonl")
    test = _rows(DATASET / "test-sessions.sealed.jsonl")
    labels = _rows(DATASET / "venue-labels.jsonl")

    assert len(development) == 2000
    assert len(test) == 700
    assert max(datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00")) for row in development) < min(
        datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00")) for row in test
    )
    assert sum(row["ambiguousObservedChoice"] for row in development) == 240
    assert sum(row["ambiguousObservedChoice"] for row in test) == 42
    assert len({row["typeCode"] for row in labels}) == 28
    assert len({row["familyCode"] for row in labels}) == 6
    assert all(row["humanTaxonomyReviewRequired"] for row in labels)
    assert all(not row["productionTrainingAllowed"] for row in labels)


def test_five_rolling_folds_never_use_future_or_test() -> None:
    development = _rows(DATASET / "development-sessions.jsonl")
    folds = _rolling_folds(development, 5)

    assert len(folds) == 5
    for train, validation in folds:
        assert max(datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00")) for row in train) <= min(
            datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00")) for row in validation
        )
        assert all(row["split"] != "test" for row in train + validation)


def test_feature_contract_excludes_ids_positions_and_outcomes() -> None:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    forbidden = {"venueId", "profileId", "position", "clicked", "bookingCompleted", "relevance"}

    assert policy["folds"] == 5
    assert forbidden.isdisjoint(policy["featureNames"])
    assert {"taxonomyTypeAffinity", "taxonomyFamilyAffinity", "visualAmbienceAffinity",
            "availabilityRatio", "proximity", "commonHourAffinity"}.issubset(policy["featureNames"])


def test_result_passes_all_requested_metrics_and_preserves_no_promotion() -> None:
    report = json.loads(RESULT.read_text(encoding="utf-8"))
    training = report["trainingMetrics"]
    test = report["testMetrics"]

    assert training["accuracy"] <= 0.90
    assert test["accuracy"] >= 0.90
    assert test["errorRate"] < 0.15
    assert test["precision"] >= 0.80
    assert test["recall"] >= 0.80
    assert test["f1"] >= 0.80
    assert test["macroFamilyPrecision"] >= 0.80
    assert test["macroFamilyRecall"] >= 0.80
    assert test["macroFamilyF1"] >= 0.80
    assert report["trainTestAccuracyGap"] <= 0.10
    assert report["qualityGatesPassed"] is True
    assert report["productionEvidence"] is False
    assert report["promotionAllowed"] is False
    assert hashlib.sha256(MODEL.read_bytes()).hexdigest() == report["modelSha256"]


def test_twelve_business_scenarios_cover_requested_flows() -> None:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    scenarios = business_scenarios(policy["featureNames"])
    result = json.loads(RESULT.read_text(encoding="utf-8"))["businessScenarioMetrics"]

    assert len(scenarios) == 12
    assert result["passed"] == 12
    assert {row["code"] for row in scenarios} == {
        "aligned-scarce-underexposed", "visual-ambience", "common-hour", "nearby-compatible",
        "specialty-type", "attribute-match", "cold-start", "quality-does-not-override-intent",
        "capacity", "price-distance", "same-family-hard-negative", "visual-cannot-override-type",
    }


def test_consumed_test_cannot_be_opened_again(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="RECOMMENDATION_DIVERSE_TEST_ALREADY_OPENED"):
        open_test(
            DATASET,
            POLICY,
            DEVELOPMENT_REPORT,
            MODEL,
            DATASET / "pretest-lock.v2.json",
            tmp_path / "result.json",
            DATASET / "test-opening-record.v2.json",
        )
