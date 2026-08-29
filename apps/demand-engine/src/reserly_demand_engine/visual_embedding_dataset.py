"""Extrae embeddings CLIP congelados para el dataset visual aprobado.

El job lee únicamente rutas contenidas en el dataset, valida SHA-256 y autorización
por fila y persiste vectores L2 normalizados. No entrena, clasifica ni calcula métricas.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

from .clip_visual_evaluation import ClipVisualManifest, HuggingFaceClipEmbedder


def _resolve(dataset_root: Path, definition_dir: Path, relative: str) -> Path:
    """Resuelve un activo sin permitir escapes fuera del dataset sintético."""

    root = dataset_root.resolve()
    path = (definition_dir / relative).resolve()
    if not path.is_relative_to(root):
        raise ValueError("VISUAL_EMBEDDING_PATH_INVALID")
    return path


def build_embedding_dataset(
    definition_path: Path,
    dataset_root: Path,
    model_manifest_path: Path,
    output_path: Path,
    batch_size: int = 8,
) -> dict[str, Any]:
    """Materializa vectores aprobados sin observar predicciones de test."""

    if batch_size < 1 or batch_size > 64:
        raise ValueError("VISUAL_EMBEDDING_BATCH_INVALID")
    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    expected_rows = {
        "approved_for_provisional_training": 120,
        "approved_for_definitive_training": 200,
    }.get(definition.get("status"))
    if (
        expected_rows is None
        or len(definition.get("rows", [])) != expected_rows
        or any(
            row.get("humanReviewStatus") != "approved"
            or row.get("developmentTrainingAllowed") is not True
            for row in definition["rows"]
        )
    ):
        raise ValueError("VISUAL_EMBEDDING_DATASET_NOT_AUTHORIZED")
    manifest = ClipVisualManifest.load(model_manifest_path)
    embedder = HuggingFaceClipEmbedder(manifest, local_files_only=True)
    ordered_rows = sorted(
        definition["rows"],
        key=lambda row: ({"train": 0, "validation": 1, "test": 2}[row["split"]], row["imageId"]),
    )
    output_rows: list[dict[str, Any]] = []
    for start in range(0, len(ordered_rows), batch_size):
        batch = ordered_rows[start : start + batch_size]
        paths = [
            _resolve(dataset_root, definition_path.parent, row["relativePath"])
            for row in batch
        ]
        for row, path in zip(batch, paths, strict=True):
            if hashlib.sha256(path.read_bytes()).hexdigest() != row["imageSha256"]:
                raise ValueError("VISUAL_EMBEDDING_HASH_MISMATCH")
        vectors = embedder.encode_images(paths)
        output_rows.extend(
            {
                "imageId": row["imageId"],
                "imageSha256": row["imageSha256"],
                "venueId": row["venueId"],
                "categoryCode": row["categoryCode"],
                "split": row["split"],
                "embedding": vector.values,
                "humanReviewStatus": "approved",
                "developmentTrainingAllowed": True,
            }
            for row, vector in zip(batch, vectors, strict=True)
        )
    dataset = {
        "schemaVersion": 1,
        "datasetVersion": definition["datasetVersion"],
        "frozenAt": definition["frozenAt"],
        "baseModelKey": manifest.modelKey,
        "baseModelRevision": manifest.revision,
        "synthetic": True,
        "containsPersonalData": False,
        "testPredictionsObservedDuringEmbedding": False,
        "rows": output_rows,
    }
    output_path.write_text(
        json.dumps(dataset, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return dataset


def run() -> None:
    """CLI para extraer el dataset de embeddings aprobado."""

    parser = argparse.ArgumentParser()
    parser.add_argument("--definition", type=Path, required=True)
    parser.add_argument("--dataset-root", type=Path, required=True)
    parser.add_argument("--model-manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--batch-size", type=int, default=8)
    args = parser.parse_args()
    result = build_embedding_dataset(
        args.definition,
        args.dataset_root,
        args.model_manifest,
        args.output,
        args.batch_size,
    )
    print(json.dumps({"rows": len(result["rows"]), "testPredictionsObserved": False}))


if __name__ == "__main__":
    run()
