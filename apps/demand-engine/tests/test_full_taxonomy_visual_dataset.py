"""Pruebas del contrato visual de cobertura taxonómica completa."""

from __future__ import annotations

import hashlib
from pathlib import Path

from reserly_demand_engine.full_taxonomy_visual_dataset import build_manifest, seal_manifest


REPO_ROOT = Path(__file__).resolve().parents[3]
TAXONOMY = REPO_ROOT / "packages/demand-contracts/catalog/venue-taxonomy.v1.json"


def test_manifest_covers_every_family_and_type_without_ocr_leakage(tmp_path: Path) -> None:
    manifest = build_manifest(TAXONOMY, tmp_path)

    assert manifest["coverage"]["familyCount"] == 23
    assert manifest["coverage"]["typeCount"] == 254
    assert len({row["typeCode"] for row in manifest["rows"]}) == 254
    assert len({row["venueId"] for row in manifest["rows"]}) == 254
    assert len({row["imageId"] for row in manifest["rows"]}) == 254
    assert all("sin logotipos" in row["prompt"] for row in manifest["rows"])
    assert all("sin letras" in row["prompt"] for row in manifest["rows"])
    assert all(row["humanReviewStatus"] == "pendingHumanReview" for row in manifest["rows"])
    assert not any(row["productionTrainingAllowed"] for row in manifest["rows"])


def test_seal_hashes_materialized_pixels_but_never_auto_approves(tmp_path: Path) -> None:
    manifest = build_manifest(TAXONOMY, tmp_path)
    first = manifest["rows"][0]
    image_path = tmp_path / first["relativePath"]
    image_path.parent.mkdir(parents=True)
    image_path.write_bytes(b"synthetic-png-placeholder")

    sealed = seal_manifest(tmp_path / "generation-manifest.json")

    sealed_first = sealed["rows"][0]
    assert sealed["materialization"]["materializedCount"] == 1
    assert sealed["materialization"]["missingCount"] == 253
    assert sealed_first["generation"]["imageSha256"] == hashlib.sha256(
        b"synthetic-png-placeholder"
    ).hexdigest()
    assert sealed_first["humanReviewStatus"] == "pendingHumanReview"
    assert sealed_first["productionTrainingAllowed"] is False
