"""Pruebas del contrato v2 development/holdout."""

from __future__ import annotations

import json
from pathlib import Path

from reserly_demand_engine.full_taxonomy_visual_holdout import build_manifest, seal_manifest


REPO_ROOT = Path(__file__).resolve().parents[3]
TAXONOMY = REPO_ROOT / "packages/demand-contracts/catalog/venue-taxonomy.v1.json"
V1_MANIFEST = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v1/generation-manifest.json"


def test_build_has_two_disjoint_views_for_all_254_types(tmp_path: Path) -> None:
    manifest = build_manifest(TAXONOMY, V1_MANIFEST, tmp_path / "v2")
    development = manifest["developmentRows"]
    holdout = manifest["holdoutRows"]

    assert len(development) == len(holdout) == 254
    assert len({row["typeCode"] for row in development}) == 254
    assert {row["typeCode"] for row in development} == {row["typeCode"] for row in holdout}
    assert {row["imageId"] for row in development}.isdisjoint(row["imageId"] for row in holdout)
    assert {row["venueId"] for row in development}.isdisjoint(row["venueId"] for row in holdout)
    assert len({row["familyCode"] for row in development}) == 23
    assert manifest["materialization"]["reusedDevelopmentCount"] == 220
    assert sum(row["generation"]["status"] == "pending" for row in development) == 34
    assert all(row["generation"]["status"] == "pending" for row in holdout)
    assert all(row["prompt"] is None for row in development if row["generation"]["status"] == "reusedConsumedDevelopment")
    assert all("No imitar" in row["prompt"] for row in development if row["generation"]["status"] == "pending")
    assert all("independientes" in row["prompt"] for row in holdout)
    assert manifest["trainingAllowed"] is False
    assert manifest["promotionAllowed"] is False


def test_seal_counts_new_bytes_without_approving_or_opening_holdout(tmp_path: Path) -> None:
    output = tmp_path / "evaluation" / "v2"
    v1_copy = tmp_path / "evaluation" / "v1" / "generation-manifest.json"
    v1_copy.parent.mkdir(parents=True)
    source = json.loads(V1_MANIFEST.read_text(encoding="utf-8"))
    for row in source["rows"]:
        row["generation"]["status"] = "pending"
        row["generation"]["imageSha256"] = None
    v1_copy.write_text(json.dumps(source), encoding="utf-8")
    manifest = build_manifest(TAXONOMY, v1_copy, output)
    development = manifest["developmentRows"][0]
    holdout = manifest["holdoutRows"][0]
    for row in (development, holdout):
        path = output / row["relativePath"]
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(row["imageId"].encode())

    sealed = seal_manifest(output / "generation-manifest.v2.json")

    assert sealed["materialization"]["newDevelopmentCount"] == 1
    assert sealed["materialization"]["holdoutCount"] == 1
    assert sealed["materialization"]["complete"] is False
    assert sealed["humanReviewComplete"] is False
    assert sealed["trainingAllowed"] is False
    assert sealed["promotionAllowed"] is False
