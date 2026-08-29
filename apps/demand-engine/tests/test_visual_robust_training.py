from __future__ import annotations

import hashlib
import json
from pathlib import Path

import pytest

from reserly_demand_engine.visual_robust_training import (
    train_robust_development_candidate,
)
from reserly_demand_engine.visual_training_diagnostics import diagnose_consumed_result


def _write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value), encoding="utf-8")


def _fixture(tmp_path: Path) -> tuple[Path, Path, Path, Path]:
    policy_path = tmp_path / "policy.json"
    definition_path = tmp_path / "definition.json"
    embeddings_path = tmp_path / "embeddings.json"
    opening_path = tmp_path / "opening.json"
    policy = {
        "schemaVersion": 1,
        "policyVersion": "robust-policy-v2",
        "algorithmVersion": "ridge-v2",
        "baseModelKey": "clip-v1",
        "baseModelRevision": "a" * 40,
        "consumedDatasetVersion": "dataset-v2",
        "categories": ["alpha", "beta"],
        "embeddingDimensions": 4,
        "stratifiedFolds": 5,
        "pcaComponentsCandidates": [2, 3],
        "ridgeCandidates": [0.1, 1.0],
        "selectionOrder": ["sourceHeldOutMacroF1"],
        "developmentGates": {
            "minimumStratifiedOofAccuracy": 0.8,
            "minimumStratifiedOofMacroF1": 0.8,
            "minimumSourceHeldOutAccuracy": 0.8,
            "maximumTrainOofAccuracyGap": 0.2,
        },
        "independentTestRequired": True,
        "automaticPromotionAllowed": False,
    }
    definitions = []
    rows = []
    for index in range(200):
        category = "alpha" if index < 100 else "beta"
        source = ["source-a", "source-b", "source-c"][index % 3]
        vector = [0.8, 0.6, 0.0, 0.0] if category == "alpha" else [0.0, 0.0, 0.8, 0.6]
        image_id = f"image-{index:03d}"
        definitions.append(
            {
                "imageId": image_id,
                "source": source,
                "humanReviewStatus": "approved",
            }
        )
        rows.append(
            {
                "imageId": image_id,
                "categoryCode": category,
                "embedding": vector,
                "humanReviewStatus": "approved",
            }
        )
    definition = {"datasetVersion": "dataset-v2", "rows": definitions}
    embeddings = {
        "datasetVersion": "dataset-v2",
        "baseModelKey": "clip-v1",
        "baseModelRevision": "a" * 40,
        "testPredictionsObservedDuringEmbedding": False,
        "rows": rows,
    }
    _write_json(policy_path, policy)
    _write_json(definition_path, definition)
    _write_json(embeddings_path, embeddings)
    _write_json(
        opening_path,
        {
            "datasetVersion": "dataset-v2",
            "openingNumber": 1,
            "openingBudget": 1,
            "remainingOpenings": 0,
            "approvedDefinitionSha256": hashlib.sha256(
                definition_path.read_bytes()
            ).hexdigest(),
            "embeddingDatasetSha256": hashlib.sha256(
                embeddings_path.read_bytes()
            ).hexdigest(),
        },
    )
    return policy_path, definition_path, embeddings_path, opening_path


def test_robust_candidate_uses_consumed_data_only_as_development(tmp_path: Path) -> None:
    policy, definition, embeddings, opening = _fixture(tmp_path)
    output = tmp_path / "artifact.json"

    artifact = train_robust_development_candidate(
        policy, definition, embeddings, opening, output
    )

    assert artifact["developmentRows"] == 200
    assert artifact["consumedTestReclassifiedAsDevelopment"] is True
    assert artifact["stratifiedOofMetrics"]["accuracy"] == 1.0
    assert artifact["sourceHeldOutMetrics"]["accuracy"] == 1.0
    assert artifact["testMetrics"] is None
    assert artifact["independentTestStatus"] == "required"
    assert artifact["promotionAllowed"] is False
    assert output.exists()


def test_robust_candidate_rejects_an_unconsumed_test(tmp_path: Path) -> None:
    policy, definition, embeddings, opening = _fixture(tmp_path)
    value = json.loads(opening.read_text(encoding="utf-8"))
    value["remainingOpenings"] = 1
    _write_json(opening, value)

    with pytest.raises(ValueError, match="VISUAL_ROBUST_CONSUMED_DATASET_CONTRACT_INVALID"):
        train_robust_development_candidate(
            policy, definition, embeddings, opening, tmp_path / "artifact.json"
        )


def test_diagnosis_reconstructs_only_the_conserved_model(tmp_path: Path) -> None:
    definition = tmp_path / "definition.json"
    embeddings = tmp_path / "embeddings.json"
    result = tmp_path / "result.json"
    opening = tmp_path / "opening.json"
    output = tmp_path / "diagnosis.json"
    metadata = []
    rows = []
    for index, (split, category, vector) in enumerate(
        [
            ("train", "alpha", [1.0, 0.0]),
            ("train", "beta", [0.0, 1.0]),
            ("validation", "alpha", [1.0, 0.0]),
            ("validation", "beta", [0.0, 1.0]),
            ("test", "alpha", [0.0, 1.0]),
            ("test", "beta", [0.0, 1.0]),
        ]
    ):
        image_id = f"image-{index}"
        metadata.append(
            {
                "imageId": image_id,
                "split": split,
                "source": "source-test" if split == "test" else "source-dev",
                "relativePath": f"images/{image_id}.png",
                "variant": f"variant-{index}",
            }
        )
        rows.append(
            {
                "imageId": image_id,
                "split": split,
                "categoryCode": category,
                "embedding": vector,
            }
        )
    _write_json(definition, {"datasetVersion": "dataset-v2", "rows": metadata})
    _write_json(embeddings, {"datasetVersion": "dataset-v2", "rows": rows})
    _write_json(
        result,
        {
            "datasetVersion": "dataset-v2",
            "testOpenedExactlyOnce": True,
            "categories": ["alpha", "beta"],
            "weights": [[1.0, 0.0], [0.0, 1.0]],
            "biases": [0.0, 0.0],
            "selectedL2": 0.1,
            "completedEpochs": 20,
            "trainMetrics": {"rows": 2, "accuracy": 1.0},
            "validationMetrics": {"accuracy": 1.0},
            "testMetrics": {"accuracy": 0.5},
            "trainTestAccuracyGap": 0.5,
        },
    )
    _write_json(
        opening,
        {"openingNumber": 1, "remainingOpenings": 0, "resultSha256": "a" * 64},
    )

    report = diagnose_consumed_result(
        definition, embeddings, result, opening, output
    )

    assert report["errorCount"] == 1
    assert report["errors"][0]["actual"] == "alpha"
    assert report["errors"][0]["predicted"] == "beta"
    assert report["alternativeTestPredictionsComputed"] is False
