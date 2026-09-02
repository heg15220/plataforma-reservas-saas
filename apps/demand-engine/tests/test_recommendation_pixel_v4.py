"""Regresión del recomendador que usa patrones de píxeles mediante CLIP."""

from __future__ import annotations

import hashlib
import json
from datetime import datetime
from pathlib import Path

import numpy as np
import pytest

from reserly_demand_engine.recommendation_pixel_dataset import generate_pixel_dataset
from reserly_demand_engine.recommendation_pixel_training import PairwiseLinearRanker, open_test, pixel_scenarios


ROOT = Path(__file__).parents[1]
SOURCE = ROOT / "evaluation/synthetic-marketplace-v1"
DIVERSE = ROOT / "evaluation/synthetic-marketplace-diverse-v2"
DATASET = ROOT / "evaluation/synthetic-marketplace-pixel-v4"
POLICY = ROOT / "policies/recommendation-pixel-personalization.v4.json"
DEVELOPMENT = ROOT / "evaluation/results/recommendation-pixel-development.v4.json"
RESULT = ROOT / "evaluation/results/recommendation-pixel-personalization.v4.json"
BASELINE_MODEL = ROOT / "models/contextual-recommender-pixel-baseline.v4.linear.json"
MULTIMODAL_MODEL = ROOT / "models/contextual-recommender-pixel-multimodal.v4.linear.json"


def _rows(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def test_generator_verifies_pixels_and_is_byte_reproducible(tmp_path: Path) -> None:
    generated = tmp_path / "pixel-v4"
    manifest = generate_pixel_dataset(SOURCE, DIVERSE, generated)

    assert manifest["counts"] == {
        "linkedApprovedImages": 70,
        "profiles": 40,
        "onboardingEvents": 80,
        "sessions": 2700,
        "developmentSessions": 2000,
        "testSessions": 700,
        "candidates": 21600,
        "families": 6,
        "types": 23,
    }
    assert manifest["pixelEvidence"]["rawPngHashesVerified"] == 70
    assert manifest["pixelEvidence"]["embeddingDimension"] == 512
    assert manifest["pixelEvidence"]["pixelsUsedIndirectlyThroughFrozenEmbeddings"] is True
    assert manifest["pixelEvidence"]["newImagesGenerated"] == 0
    for name in (
        "visual-linkage.jsonl", "visual-onboarding-events.jsonl",
        "development-sessions.jsonl", "test-sessions.sealed.jsonl",
    ):
        assert _sha(generated / name) == _sha(DATASET / name)


def test_first_visual_affinity_recomputes_from_clip_pixels_and_prior_choices() -> None:
    first = _rows(DATASET / "development-sessions.jsonl")[0]
    onboarding = [
        row for row in _rows(DATASET / "visual-onboarding-events.jsonl")
        if row["profileId"] == first["profileId"]
    ]
    embedding_artifact = json.loads(
        (SOURCE / "visual-training-dataset-v2/approved-clip-embeddings.json").read_text(encoding="utf-8")
    )
    embeddings = {}
    for row in embedding_artifact["rows"]:
        vector = np.asarray(row["embedding"], dtype=np.float64)
        embeddings[row["venueId"]] = vector / np.linalg.norm(vector)
    profile = sum((embeddings[row["venueId"]] for row in onboarding), np.zeros(512))
    profile /= np.linalg.norm(profile)

    assert first["visualProfileEvidenceCount"] == 2
    for candidate in first["candidates"]:
        cosine = float(np.dot(profile, embeddings[candidate["venueId"]]))
        expected = max(0.0, min(1.0, (cosine - 0.45) / 0.55))
        assert candidate["features"]["pixelVisualAffinity"] == pytest.approx(expected, abs=1e-8)


def test_sessions_are_strictly_temporal_and_history_never_decreases() -> None:
    sessions = _rows(DATASET / "development-sessions.jsonl") + _rows(DATASET / "test-sessions.sealed.jsonl")
    times = [datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00")) for row in sessions]

    assert all(left < right for left, right in zip(times, times[1:], strict=False))
    assert max(times[:2000]) < min(times[2000:])
    by_profile: dict[str, list[dict]] = {}
    for session in sessions:
        by_profile.setdefault(session["profileId"], []).append(session)
    for profile_sessions in by_profile.values():
        evidence = [row["visualProfileEvidenceCount"] for row in profile_sessions]
        assert evidence == sorted(evidence)
        assert evidence[0] >= 2
    assert all(row["visualProfileBuiltFromMaturePastOnly"] for row in sessions)


def test_ablation_contract_isolated_pixel_features() -> None:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    forbidden = {"venueId", "profileId", "position", "clicked", "bookingCompleted", "relevance"}

    assert "pixelVisualAffinity" not in policy["baselineFeatureNames"]
    assert "pixelVisualAffinity" in policy["multimodalFeatureNames"]
    assert forbidden.isdisjoint(policy["baselineFeatureNames"])
    assert forbidden.isdisjoint(policy["multimodalFeatureNames"])
    assert set(policy["multimodalFeatureNames"]) - set(policy["baselineFeatureNames"]) == {
        "pixelVisualAffinity", "pixelVisualHistoryConfidence"
    }


def test_multimodal_model_learns_positive_pixel_contribution() -> None:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    model = PairwiseLinearRanker.load_model(MULTIMODAL_MODEL)
    coefficients = dict(zip(policy["multimodalFeatureNames"], model.coefficients, strict=True))

    assert coefficients["pixelVisualAffinity"] > 0
    assert coefficients["pixelVisualAffinity"] == max(coefficients.values())


def test_sealed_test_proves_pixel_uplift_and_requested_quality() -> None:
    report = json.loads(RESULT.read_text(encoding="utf-8"))
    baseline = report["baselineTestMetrics"]
    multimodal = report["multimodalTestMetrics"]

    assert report["pixelPatternsUsed"] is True
    assert report["testVisualAccuracyUplift"] >= 0.10
    assert multimodal["accuracy"] > baseline["accuracy"]
    assert report["multimodalTrainingMetrics"]["accuracy"] <= 0.90
    assert multimodal["accuracy"] >= 0.90
    assert multimodal["errorRate"] < 0.15
    assert multimodal["precision"] >= 0.80
    assert multimodal["recall"] >= 0.80
    assert multimodal["f1"] >= 0.80
    assert report["multimodalTrainTestAccuracyGap"] <= 0.10
    assert report["qualityGatesPassed"] is True
    assert report["productionEvidence"] is False
    assert report["promotionAllowed"] is False


def test_pixel_scenarios_and_test_reopening_are_fail_closed(tmp_path: Path) -> None:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    scenarios = pixel_scenarios(policy["multimodalFeatureNames"])
    report = json.loads(RESULT.read_text(encoding="utf-8"))

    assert len(scenarios) == 8
    assert report["pixelScenarioMetrics"]["passed"] == 8
    with pytest.raises(ValueError, match="RECOMMENDATION_PIXEL_TEST_ALREADY_OPENED"):
        open_test(
            DATASET, POLICY, DEVELOPMENT, BASELINE_MODEL, MULTIMODAL_MODEL,
            DATASET / "pretest-lock.v4.json", tmp_path / "result.json",
            DATASET / "test-opening-record.v4.json",
        )


def test_invalid_v3_is_preserved_but_cannot_be_used_as_evidence() -> None:
    invalidation = json.loads(
        (ROOT / "evaluation/synthetic-marketplace-pixel-v3/invalidation-record.v3.json").read_text(encoding="utf-8")
    )

    assert invalidation["status"] == "invalidated"
    assert invalidation["metricsUsable"] is False
    assert invalidation["promotionAllowed"] is False
    assert invalidation["replacementDatasetVersion"] == "synthetic-marketplace-pixel-personalization-v4"
