"""Contrato del corpus taxonómico multivista v3."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_multiview_v3 import build_manifest


REPO_ROOT = Path(__file__).resolve().parents[3]
EVALUATION = REPO_ROOT / "apps/demand-engine/evaluation"
TAXONOMY = REPO_ROOT / "packages/demand-contracts/catalog/venue-taxonomy.v1.json"
V2 = EVALUATION / "synthetic-marketplace-full-taxonomy-visual-v2"


def _build(tmp_path: Path) -> dict:
    return build_manifest(
        TAXONOMY,
        V2 / "generation-manifest.v2.json",
        V2 / "test-opening-record.v2.json",
        tmp_path,
    )


def test_v3_has_three_development_views_and_new_holdout_per_type(tmp_path: Path) -> None:
    manifest = _build(tmp_path)
    assert len(manifest["developmentRows"]) == 762
    assert len(manifest["holdoutRows"]) == 254
    assert manifest["expectedCoverage"]["newImageCount"] == 508
    by_type: dict[str, set[str]] = {}
    for row in manifest["developmentRows"]:
        by_type.setdefault(row["typeCode"], set()).add(row["developmentView"])
    assert len(by_type) == 254
    assert all(views == {"A", "B", "C"} for views in by_type.values())
    assert len({row["familyCode"] for row in manifest["holdoutRows"]}) == 23


def test_consumed_v2_holdout_is_only_development_and_v3_is_disjoint(tmp_path: Path) -> None:
    manifest = _build(tmp_path)
    development_ids = {row["imageId"] for row in manifest["developmentRows"]}
    development_venues = {row["venueId"] for row in manifest["developmentRows"]}
    holdout_ids = {row["imageId"] for row in manifest["holdoutRows"]}
    holdout_venues = {row["venueId"] for row in manifest["holdoutRows"]}
    assert development_ids.isdisjoint(holdout_ids)
    assert development_venues.isdisjoint(holdout_venues)
    reused_b = [row for row in manifest["developmentRows"] if row["developmentView"] == "B"]
    assert len(reused_b) == 254
    assert all(row["testEligible"] is False for row in reused_b)
    assert all("v2-holdout-as-v3-development" in row["generation"]["mode"] for row in reused_b)
    assert all(row["testEligible"] is True for row in manifest["holdoutRows"])
    assert all(row["testEvaluationAllowed"] is False for row in manifest["holdoutRows"])


def test_archetypes_and_people_policy_are_exhaustive_and_safe(tmp_path: Path) -> None:
    manifest = _build(tmp_path)
    archetypes = {row["visualArchetype"]["code"] for row in manifest["holdoutRows"]}
    assert len(archetypes) == 38
    assert manifest["protocol"]["archetypeAuxiliaryTargetMustBePredictedFromPixelsAtInference"] is True
    assert manifest["protocol"]["promptTypeFamilyOrTrueArchetypeAsFeatureForbidden"] is True
    for row in manifest["developmentRows"] + manifest["holdoutRows"]:
        policy = row["peoplePolicy"]
        assert policy["identifiableFacesForbidden"] is True
        assert policy["minorsForbidden"] is True
        assert policy["patientsOrSensitiveSituationsForbidden"] is True
        assert policy["biometricOrSensitiveInferenceForbidden"] is True
        if 119 <= row["sourceId"] <= 159 or 170 <= row["sourceId"] <= 172 or row["sourceId"] == 40:
            assert policy["allowed"] is False
            assert policy["maxAdults"] == 0


def test_holdout_prompts_are_independent_and_do_not_embed_labels_as_text(tmp_path: Path) -> None:
    manifest = _build(tmp_path)
    development_c = {row["typeCode"]: row for row in manifest["developmentRows"] if row["developmentView"] == "C"}
    for holdout in manifest["holdoutRows"]:
        development = development_c[holdout["typeCode"]]
        assert holdout["prompt"] != development["prompt"]
        assert "completamente nuevo y no relacionado" in holdout["prompt"]
        assert "Sin logotipos" in holdout["prompt"]
        assert "etiqueta de categoría" in holdout["prompt"]


def test_build_fails_if_v2_holdout_was_not_consumed(tmp_path: Path) -> None:
    invalid = tmp_path / "opening.json"
    invalid.write_text(json.dumps({"consumed": 0, "reopenAllowed": False}), encoding="utf-8")
    with pytest.raises(ValueError, match="V2_HOLDOUT_NOT_CONSUMED"):
        build_manifest(TAXONOMY, V2 / "generation-manifest.v2.json", invalid, tmp_path / "out")
