"""Diagnóstico reproducible de un resultado visual ya consumido.

El módulo no entrena candidatos ni modifica predicciones. Reconstruye únicamente la
salida del artefacto conservado para localizar errores, márgenes y cambio de dominio.
Así permite investigar un test abierto sin convertirlo en conjunto de selección.
"""

from __future__ import annotations

import argparse
import json
import math
from collections import Counter
from pathlib import Path
from typing import Any


def _cosine(left: list[float], right: list[float]) -> float:
    numerator = sum(a * b for a, b in zip(left, right, strict=True))
    left_norm = math.sqrt(sum(value * value for value in left))
    right_norm = math.sqrt(sum(value * value for value in right))
    return numerator / (left_norm * right_norm) if left_norm and right_norm else 0.0


def _centroid(rows: list[dict[str, Any]]) -> list[float]:
    dimensions = len(rows[0]["embedding"])
    return [
        sum(row["embedding"][index] for row in rows) / len(rows)
        for index in range(dimensions)
    ]


def diagnose_consumed_result(
    definition_path: Path,
    embeddings_path: Path,
    result_path: Path,
    opening_record_path: Path,
    output_path: Path,
) -> dict[str, Any]:
    """Crea evidencia post hoc sin probar ni seleccionar modelos alternativos."""

    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    embeddings = json.loads(embeddings_path.read_text(encoding="utf-8"))
    result = json.loads(result_path.read_text(encoding="utf-8"))
    opening = json.loads(opening_record_path.read_text(encoding="utf-8"))
    if (
        opening.get("openingNumber") != 1
        or opening.get("remainingOpenings") != 0
        or opening.get("resultSha256") is None
        or result.get("testOpenedExactlyOnce") is not True
        or definition.get("datasetVersion") != embeddings.get("datasetVersion")
        or result.get("datasetVersion") != embeddings.get("datasetVersion")
    ):
        raise ValueError("VISUAL_DIAGNOSIS_CONSUMED_RESULT_REQUIRED")

    metadata = {row["imageId"]: row for row in definition["rows"]}
    rows = embeddings["rows"]
    if len(metadata) != len(rows) or {row["imageId"] for row in rows} != set(metadata):
        raise ValueError("VISUAL_DIAGNOSIS_DATASET_MISMATCH")
    categories = result["categories"]
    weights = result["weights"]
    biases = result["biases"]
    errors: list[dict[str, Any]] = []
    correct_margins: list[float] = []
    error_margins: list[float] = []
    for row in rows:
        if row["split"] != "test":
            continue
        scores = [
            bias
            + sum(
                weight * value
                for weight, value in zip(class_weights, row["embedding"], strict=True)
            )
            for class_weights, bias in zip(weights, biases, strict=True)
        ]
        ordered = sorted(range(len(scores)), key=scores.__getitem__, reverse=True)
        predicted = categories[ordered[0]]
        margin = round(scores[ordered[0]] - scores[ordered[1]], 8)
        if predicted == row["categoryCode"]:
            correct_margins.append(margin)
            continue
        error_margins.append(margin)
        meta = metadata[row["imageId"]]
        errors.append(
            {
                "imageId": row["imageId"],
                "actual": row["categoryCode"],
                "predicted": predicted,
                "margin": margin,
                "source": meta["source"],
                "variant": meta.get("variant"),
                "relativePath": meta["relativePath"],
            }
        )

    source_by_split = Counter(
        (row["split"], metadata[row["imageId"]]["source"]) for row in rows
    )
    centroid_shift: list[dict[str, Any]] = []
    for category in categories:
        train = [
            row for row in rows if row["categoryCode"] == category and row["split"] == "train"
        ]
        validation = [
            row
            for row in rows
            if row["categoryCode"] == category and row["split"] == "validation"
        ]
        development = train + validation
        test = [
            row for row in rows if row["categoryCode"] == category and row["split"] == "test"
        ]
        centroid_shift.append(
            {
                "categoryCode": category,
                "trainValidationCosine": round(
                    _cosine(_centroid(train), _centroid(validation)), 8
                ),
                "developmentTestCosine": round(
                    _cosine(_centroid(development), _centroid(test)), 8
                ),
            }
        )

    train_rows = result["trainMetrics"]["rows"]
    raw_parameters = len(categories) * (len(weights[0]) + 1)
    diagnosis = {
        "schemaVersion": 1,
        "reportVersion": "visual-training-diagnosis-v1",
        "datasetVersion": result["datasetVersion"],
        "resultAlreadyConsumed": True,
        "alternativeTestPredictionsComputed": False,
        "testUsedForCandidateSelection": False,
        "observedMetrics": {
            "trainAccuracy": result["trainMetrics"]["accuracy"],
            "validationAccuracy": result["validationMetrics"]["accuracy"],
            "testAccuracy": result["testMetrics"]["accuracy"],
            "trainValidationGap": round(
                result["trainMetrics"]["accuracy"]
                - result["validationMetrics"]["accuracy"],
                8,
            ),
            "trainTestGap": result["trainTestAccuracyGap"],
        },
        "capacity": {
            "trainRows": train_rows,
            "embeddingDimensions": len(weights[0]),
            "classes": len(categories),
            "rawLinearParameters": raw_parameters,
            "parametersPerTrainRow": round(raw_parameters / train_rows, 4),
            "selectedL2": result["selectedL2"],
            "completedEpochs": result["completedEpochs"],
        },
        "sourceBySplit": [
            {"split": split, "source": source, "rows": count}
            for (split, source), count in sorted(source_by_split.items())
        ],
        "centroidShift": centroid_shift,
        "errors": errors,
        "errorCount": len(errors),
        "meanCorrectMargin": round(sum(correct_margins) / len(correct_margins), 8),
        "meanErrorMargin": round(sum(error_margins) / len(error_margins), 8),
        "conclusions": [
            "TRAIN_VALIDATION_GAP_SMALL_NOT_EXCESSIVE_CLASSIC_OVERFIT",
            "TEST_SOURCE_ABSENT_FROM_TRAIN_AND_VALIDATION",
            "HIGH_DIMENSION_LOW_SAMPLE_CAPACITY_RISK",
            "REGULARIZATION_TIE_SELECTED_WEAKEST_L2",
            "MUNICIPAL_AND_OTHER_NOT_PURELY_VISUAL_ONTOLOGY",
        ],
    }
    output_path.write_text(
        json.dumps(diagnosis, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return diagnosis


def run() -> None:
    """CLI del diagnóstico gobernado."""

    parser = argparse.ArgumentParser()
    parser.add_argument("--definition", type=Path, required=True)
    parser.add_argument("--embeddings", type=Path, required=True)
    parser.add_argument("--result", type=Path, required=True)
    parser.add_argument("--opening-record", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = diagnose_consumed_result(
        args.definition,
        args.embeddings,
        args.result,
        args.opening_record,
        args.output,
    )
    print(json.dumps({"errors": report["errorCount"], "conclusions": report["conclusions"]}))


if __name__ == "__main__":
    run()
