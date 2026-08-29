"""Carga del catálogo de locales y reetiquetado gobernado de imágenes existentes.

El módulo no cambia etiquetas históricas ni abre tests consumidos. Produce propuestas
pendientes para reutilizar activos únicamente como desarrollo después de revisión humana.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Literal

from pydantic import Field
from reserly_demand_contracts.venue_taxonomy_v1 import (
    Slug,
    StrictTaxonomyModel,
    VenueTaxonomyV1,
)


class RelabelWorkItem(StrictTaxonomyModel):
    """Propuesta no autoritativa para revisar un activo visual ya consumido."""

    taxonomyVersion: Literal["venue-taxonomy.v1"]
    sourceDatasetVersion: Literal["visual-category-dataset-v2-definitive-200"]
    imageId: str = Field(pattern=r"^[a-f0-9-]{36}$")
    venueId: str = Field(pattern=r"^[a-f0-9-]{36}$")
    imageSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    legacyCategoryCode: Slug
    originalSplit: Literal["train", "validation", "test"]
    proposedTypeCodes: list[Slug] = Field(max_length=8)
    proposedOperatorTypeCode: Literal["public-municipal"] | None
    mappingKind: Literal["canonicalType", "operatorAttribute", "compositeRequiresReview"]
    reviewStatus: Literal["pendingHumanReview"]
    allowedUse: Literal["developmentRelabelingOnly"]
    testEligible: Literal[False]
    productionTrainingAllowed: Literal[False]


@dataclass(frozen=True, slots=True)
class RelabelBundle:
    """Filas y manifiesto content-addressed listos para persistencia reproducible."""

    rows: tuple[RelabelWorkItem, ...]
    manifest: dict[str, object]


def load_taxonomy(path: Path) -> VenueTaxonomyV1:
    """Carga el catálogo estricto; cualquier extensión o tipo incorrecto falla cerrado."""
    return VenueTaxonomyV1.model_validate_json(path.read_text(encoding="utf-8"))


def build_relabel_bundle(
    taxonomy_path: Path,
    approved_definition_path: Path,
) -> RelabelBundle:
    """Convierte 200 activos aprobados en propuestas de desarrollo, nunca en un test nuevo."""
    taxonomy_bytes = taxonomy_path.read_bytes()
    definition_bytes = approved_definition_path.read_bytes()
    taxonomy = VenueTaxonomyV1.model_validate_json(taxonomy_bytes)
    definition = json.loads(definition_bytes)
    if (
        definition.get("datasetVersion") != "visual-category-dataset-v2-definitive-200"
        or definition.get("status") != "approved_for_definitive_training"
        or len(definition.get("rows", [])) != 200
    ):
        raise ValueError("VENUE_TAXONOMY_RELABEL_SOURCE_INVALID")

    compatibility = {item.legacyCategoryCode: item for item in taxonomy.legacyCompatibility}
    rows: list[RelabelWorkItem] = []
    seen_images: set[str] = set()
    for source_row in definition["rows"]:
        image_id = source_row.get("imageId")
        category = source_row.get("categoryCode")
        if (
            source_row.get("humanReviewStatus") != "approved"
            or source_row.get("developmentTrainingAllowed") is not True
            or source_row.get("productionTrainingAllowed") is not False
            or image_id in seen_images
            or category not in compatibility
        ):
            raise ValueError("VENUE_TAXONOMY_RELABEL_ROW_INVALID")
        seen_images.add(image_id)
        mapping = compatibility[category]
        rows.append(RelabelWorkItem(
            taxonomyVersion=taxonomy.taxonomyVersion,
            sourceDatasetVersion=definition["datasetVersion"],
            imageId=image_id,
            venueId=source_row["venueId"],
            imageSha256=source_row["imageSha256"],
            legacyCategoryCode=category,
            originalSplit=source_row["split"],
            proposedTypeCodes=list(mapping.targetTypeCodes),
            proposedOperatorTypeCode=mapping.operatorTypeCode,
            mappingKind=mapping.mappingKind,
            reviewStatus="pendingHumanReview",
            allowedUse="developmentRelabelingOnly",
            testEligible=False,
            productionTrainingAllowed=False,
        ))

    counts = Counter(item.legacyCategoryCode for item in rows)
    split_counts = Counter(item.originalSplit for item in rows)
    if set(counts) != set(compatibility) or any(value != 25 for value in counts.values()):
        raise ValueError("VENUE_TAXONOMY_RELABEL_CATEGORY_COVERAGE_INVALID")
    serialized_rows = "".join(
        json.dumps(item.model_dump(mode="json"), ensure_ascii=False, sort_keys=True) + "\n"
        for item in rows
    ).encode("utf-8")
    manifest = {
        "schemaVersion": 1,
        "worklistVersion": "venue-taxonomy-relabel.v1",
        "taxonomyVersion": taxonomy.taxonomyVersion,
        "taxonomySha256": hashlib.sha256(taxonomy_bytes).hexdigest(),
        "sourceDatasetVersion": definition["datasetVersion"],
        "sourceDefinitionSha256": hashlib.sha256(definition_bytes).hexdigest(),
        "rowCount": len(rows),
        "categoryCounts": dict(sorted(counts.items())),
        "originalSplitCounts": dict(sorted(split_counts.items())),
        "pendingHumanReviewCount": len(rows),
        "developmentRelabelingOnly": True,
        "testEligibleCount": 0,
        "productionTrainingAllowedCount": 0,
        "worklistSha256": hashlib.sha256(serialized_rows).hexdigest(),
        "municipalModeledAsOperatorAttribute": True,
        "otherRequiresPhysicalTypeReview": True,
        "newImagesGenerated": 0,
    }
    return RelabelBundle(rows=tuple(rows), manifest=manifest)


def write_relabel_bundle(bundle: RelabelBundle, worklist_path: Path, manifest_path: Path) -> None:
    """Persiste resultados deterministas sin modificar definición, imágenes ni hashes fuente."""
    worklist_path.parent.mkdir(parents=True, exist_ok=True)
    payload = "".join(
        json.dumps(item.model_dump(mode="json"), ensure_ascii=False, sort_keys=True) + "\n"
        for item in bundle.rows
    ).encode("utf-8")
    # Escribir bytes evita que Windows cambie LF por CRLF después de calcular el SHA-256.
    worklist_path.write_bytes(payload)
    manifest_path.write_text(
        json.dumps(bundle.manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def run() -> None:
    """CLI explícito para regenerar y auditar la cola de revisión."""
    parser = argparse.ArgumentParser()
    parser.add_argument("taxonomy", type=Path)
    parser.add_argument("approved_definition", type=Path)
    parser.add_argument("worklist", type=Path)
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    bundle = build_relabel_bundle(args.taxonomy, args.approved_definition)
    write_relabel_bundle(bundle, args.worklist, args.manifest)


if __name__ == "__main__":
    run()
