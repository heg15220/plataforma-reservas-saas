"""Selección 5-fold y apertura única del ranker conjunto escalado v9."""

from __future__ import annotations

import argparse
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np

from .recommendation_joint_scale_dataset import FEATURE_NAMES, FLAG_NAMES


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, value: Any, compact: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, separators=(",", ":") if compact else None,
                   indent=None if compact else 2) + "\n",
        encoding="utf-8",
    )


def _load(path: Path) -> dict[str, np.ndarray]:
    with np.load(path, allow_pickle=False) as artifact:
        return {name: np.asarray(artifact[name]) for name in artifact.files}


@dataclass(frozen=True)
class PairwiseLogisticRanker:
    """Ranker lineal portable con pérdida logística pairwise."""

    feature_names: tuple[str, ...]
    mean: np.ndarray
    std: np.ndarray
    weights: np.ndarray
    regularization: float

    def predict(self, matrix: np.ndarray) -> np.ndarray:
        values = np.asarray(matrix, dtype=np.float64)
        return ((values - self.mean) / self.std) @ self.weights

    def to_dict(self, model_key: str) -> dict[str, Any]:
        return {
            "schemaVersion": 1,
            "modelKey": model_key,
            "algorithm": "pairwise-logistic-newton-ranking-on-engineered-features",
            "featureNames": list(self.feature_names),
            "featureMean": self.mean.tolist(),
            "featureStd": self.std.tolist(),
            "weights": self.weights.tolist(),
            "regularization": self.regularization,
            "rawIdentifiersAreFeatures": False,
            "candidatePositionIsFeature": False,
            "productionEvidence": False,
            "promotionAllowed": False,
        }

    @classmethod
    def from_dict(cls, raw: dict[str, Any]) -> "PairwiseLogisticRanker":
        if raw.get("algorithm") != "pairwise-logistic-newton-ranking-on-engineered-features":
            raise ValueError("RECOMMENDATION_JOINT_MODEL_FORMAT_INVALID")
        return cls(
            tuple(raw["featureNames"]), np.asarray(raw["featureMean"]),
            np.asarray(raw["featureStd"]), np.asarray(raw["weights"]),
            float(raw["regularization"]),
        )


def _fit(
    features: np.ndarray, positive: np.ndarray, feature_indices: np.ndarray,
    regularization: float,
) -> PairwiseLogisticRanker:
    selected = np.asarray(features[:, :, feature_indices], dtype=np.float64)
    flattened = selected.reshape(-1, selected.shape[-1])
    mean = flattened.mean(axis=0)
    std = flattened.std(axis=0).clip(min=1e-6)
    normalized = (selected - mean) / std
    positives = normalized[np.arange(len(normalized)), positive]
    differences = positives[:, None, :] - normalized
    mask = np.arange(normalized.shape[1])[None, :] != positive[:, None]
    matrix = differences[mask]
    weights = np.zeros(matrix.shape[1], dtype=np.float64)
    identity = np.eye(matrix.shape[1])
    for _ in range(12):
        logits = np.clip(matrix @ weights, -30, 30)
        probability = 1 / (1 + np.exp(-logits))
        gradient = matrix.T @ (probability - 1) + regularization * weights
        curvature = probability * (1 - probability)
        hessian = (matrix.T * curvature) @ matrix + regularization * identity
        step = np.linalg.solve(hessian, gradient)
        weights -= step
        if float(np.linalg.norm(step)) < 1e-7:
            break
    return PairwiseLogisticRanker(
        tuple(FEATURE_NAMES[index] for index in feature_indices),
        mean, std, weights, regularization,
    )


def _metrics(
    model: PairwiseLogisticRanker, features: np.ndarray, positive: np.ndarray,
) -> dict[str, Any]:
    indices = np.asarray([FEATURE_NAMES.index(name) for name in model.feature_names])
    scores = model.predict(features[:, :, indices])
    predicted = np.argmax(scores, axis=1)
    correct = predicted == positive
    top3 = np.argpartition(scores, -3, axis=1)[:, -3:]
    recall_at_3 = np.mean([positive[row] in top3[row] for row in range(len(positive))])
    accuracy = float(correct.mean())
    return {
        "sessions": len(positive),
        "correctTop1": int(correct.sum()),
        "accuracy": round(accuracy, 8),
        "errorRate": round(1 - accuracy, 8),
        "precision": round(accuracy, 8),
        "recall": round(accuracy, 8),
        "f1": round(accuracy, 8),
        "recallAt3": round(float(recall_at_3), 8),
    }


def _slice_metrics(
    model: PairwiseLogisticRanker, data: dict[str, np.ndarray]
) -> dict[str, Any]:
    result = {}
    for flag_index, name in enumerate(FLAG_NAMES):
        mask = data["scenarioFlags"][:, flag_index].astype(bool)
        if not mask.any():
            result[name] = {"sessions": 0, "accuracy": None, "f1": None}
            continue
        metric = _metrics(model, data["features"][mask], data["positiveIndices"][mask])
        result[name] = {
            "sessions": metric["sessions"],
            "accuracy": metric["accuracy"],
            "f1": metric["f1"],
        }
    return result


def _rolling_folds(session_count: int) -> list[tuple[np.ndarray, np.ndarray]]:
    """Cinco folds expanding-window sobre seis bloques cronológicos."""

    blocks = np.array_split(np.arange(session_count), 6)
    return [
        (np.concatenate(blocks[:index]), blocks[index]) for index in range(1, 6)
    ]


def _select(
    data: dict[str, np.ndarray], indices: np.ndarray, policy: dict[str, Any],
) -> tuple[dict[str, Any], PairwiseLogisticRanker]:
    candidates = []
    folds = _rolling_folds(len(data["positiveIndices"]))
    for regularization in policy["regularizationCandidates"]:
        fold_metrics = []
        for training, validation in folds:
            model = _fit(
                data["features"][training], data["positiveIndices"][training],
                indices, float(regularization),
            )
            fold_metrics.append(
                _metrics(model, data["features"][validation], data["positiveIndices"][validation])
            )
        aggregate = {
            key: round(float(np.mean([row[key] for row in fold_metrics])), 8)
            for key in ("accuracy", "errorRate", "precision", "recall", "f1", "recallAt3")
        }
        candidates.append(
            {"regularization": regularization, "foldMetrics": fold_metrics, "meanMetrics": aggregate}
        )
    eligible = [
        row for row in candidates
        if row["meanMetrics"]["accuracy"] <= policy["gates"]["maximumDevelopmentAccuracy"]
    ] or candidates
    selected = max(
        eligible,
        key=lambda row: (
            row["meanMetrics"]["f1"], row["meanMetrics"]["recallAt3"],
            -float(row["regularization"]),
        ),
    )
    model = _fit(
        data["features"], data["positiveIndices"], indices,
        float(selected["regularization"]),
    )
    return {
        "featureNames": [FEATURE_NAMES[index] for index in indices],
        "candidates": candidates,
        "selectedRegularization": selected["regularization"],
        "trainingMetrics": selected["meanMetrics"],
        "inSampleDiagnostics": _metrics(
            model, data["features"], data["positiveIndices"]
        ),
    }, model


def _validate_policy(policy: dict[str, Any], manifest: dict[str, Any]) -> None:
    if policy.get("datasetVersion") != manifest.get("datasetVersion"):
        raise ValueError("RECOMMENDATION_JOINT_POLICY_DATASET_MISMATCH")
    if policy.get("folds") != 5 or policy.get("automaticPromotionAllowed") is not False:
        raise ValueError("RECOMMENDATION_JOINT_POLICY_INVALID")
    if policy["contextFeatureNames"] != list(FEATURE_NAMES[:23]):
        raise ValueError("RECOMMENDATION_JOINT_CONTEXT_ABLATION_INVALID")
    if policy["jointFeatureNames"] != list(FEATURE_NAMES):
        raise ValueError("RECOMMENDATION_JOINT_FEATURE_CONTRACT_INVALID")


def select_models(
    dataset: Path, policy_path: Path, report_path: Path,
    context_model_path: Path, joint_model_path: Path, lock_path: Path,
) -> dict[str, Any]:
    """Selecciona ambos brazos sin leer el NPZ test sellado."""

    if any(path.exists() for path in (report_path, context_model_path, joint_model_path, lock_path)):
        raise ValueError("RECOMMENDATION_JOINT_SELECTION_ALREADY_EXISTS")
    manifest = _read_json(dataset / "manifest.json")
    policy = _read_json(policy_path)
    _validate_policy(policy, manifest)
    development_path = dataset / "development.npz"
    if _sha256(development_path) != manifest["files"]["development"]["sha256"]:
        raise ValueError("RECOMMENDATION_JOINT_DEVELOPMENT_HASH_MISMATCH")
    development = _load(development_path)
    if development["features"].shape != (18000, 12, len(FEATURE_NAMES)):
        raise ValueError("RECOMMENDATION_JOINT_DEVELOPMENT_SHAPE_INVALID")
    context_indices = np.arange(23)
    joint_indices = np.arange(len(FEATURE_NAMES))
    context, context_model = _select(development, context_indices, policy)
    joint, joint_model = _select(development, joint_indices, policy)
    _write_json(context_model_path, context_model.to_dict("contextual-scale-ablation-v10"), compact=True)
    _write_json(joint_model_path, joint_model.to_dict("joint-context-visual-ranker-v10"), compact=True)
    slices = _slice_metrics(joint_model, development)
    report = {
        "schemaVersion": 1,
        "reportVersion": "recommendation-joint-scale-development-v10",
        "datasetVersion": manifest["datasetVersion"],
        "foldStrategy": "five-fold-expanding-window-rolling-origin",
        "folds": 5,
        "counts": manifest["counts"],
        "contextAblation": context,
        "jointModel": joint,
        "developmentVisualAccuracyUplift": round(
            joint["trainingMetrics"]["accuracy"] - context["trainingMetrics"]["accuracy"], 8
        ),
        "developmentSliceMetrics": slices,
        "singleScoringModel": True,
        "visualClassifierUsedAsOfflineFeatureProducer": True,
        "testOpened": False,
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write_json(report_path, report)
    lock = {
        "schemaVersion": 1,
        "status": "joint-and-context-selected-test-sealed",
        "developmentSha256": _sha256(development_path),
        "sealedTestSha256": manifest["files"]["sealedTest"]["sha256"],
        "manifestSha256": _sha256(dataset / "manifest.json"),
        "policySha256": _sha256(policy_path),
        "developmentReportSha256": _sha256(report_path),
        "contextModelSha256": _sha256(context_model_path),
        "jointModelSha256": _sha256(joint_model_path),
        "testOpenBudget": 1,
        "testOpenCount": 0,
    }
    _write_json(lock_path, lock)
    return report


def open_test(
    dataset: Path, policy_path: Path, report_path: Path,
    context_model_path: Path, joint_model_path: Path, lock_path: Path,
    result_path: Path, opening_path: Path,
) -> dict[str, Any]:
    """Abre una sola vez el test temporal y conserva el resultado aunque falle."""

    if result_path.exists() or opening_path.exists():
        raise ValueError("RECOMMENDATION_JOINT_TEST_ALREADY_OPENED")
    manifest = _read_json(dataset / "manifest.json")
    policy = _read_json(policy_path)
    _validate_policy(policy, manifest)
    lock = _read_json(lock_path)
    expected = {
        "developmentSha256": _sha256(dataset / "development.npz"),
        "sealedTestSha256": _sha256(dataset / "test.sealed.npz"),
        "manifestSha256": _sha256(dataset / "manifest.json"),
        "policySha256": _sha256(policy_path),
        "developmentReportSha256": _sha256(report_path),
        "contextModelSha256": _sha256(context_model_path),
        "jointModelSha256": _sha256(joint_model_path),
    }
    if lock.get("status") != "joint-and-context-selected-test-sealed" or any(
        lock.get(key) != value for key, value in expected.items()
    ):
        raise ValueError("RECOMMENDATION_JOINT_PRETEST_HASH_MISMATCH")
    test = _load(dataset / "test.sealed.npz")
    context_model = PairwiseLogisticRanker.from_dict(_read_json(context_model_path))
    joint_model = PairwiseLogisticRanker.from_dict(_read_json(joint_model_path))
    context_metrics = _metrics(context_model, test["features"], test["positiveIndices"])
    joint_metrics = _metrics(joint_model, test["features"], test["positiveIndices"])
    slices = _slice_metrics(joint_model, test)
    development = _read_json(report_path)
    training = development["jointModel"]["trainingMetrics"]
    uplift = round(joint_metrics["accuracy"] - context_metrics["accuracy"], 8)
    gap = round(abs(training["accuracy"] - joint_metrics["accuracy"]), 8)
    gates = policy["gates"]
    passed = (
        training["accuracy"] <= gates["maximumDevelopmentAccuracy"]
        and joint_metrics["accuracy"] >= gates["minimumTestAccuracy"]
        and joint_metrics["errorRate"] < gates["maximumTestErrorExclusive"]
        and joint_metrics["precision"] >= gates["minimumTestPrecision"]
        and joint_metrics["recall"] >= gates["minimumTestRecall"]
        and joint_metrics["f1"] >= gates["minimumTestF1"]
        and joint_metrics["recallAt3"] >= gates["minimumTestRecallAt3"]
        and uplift >= gates["minimumVisualAccuracyUplift"]
        and gap <= gates["maximumDevelopmentTestGap"]
        and slices["locationSensitive"]["accuracy"] >= gates["minimumLocationSliceAccuracy"]
        and slices["scarceAligned"]["accuracy"] >= gates["minimumScarcitySliceAccuracy"]
        and slices["visualChallenge"]["accuracy"] >= gates["minimumVisualChallengeAccuracy"]
    )
    result = {
        "schemaVersion": 1,
        "reportVersion": "recommendation-joint-scale-test-v10",
        "datasetVersion": manifest["datasetVersion"],
        "counts": manifest["counts"],
        "contextAblationTestMetrics": context_metrics,
        "jointTrainingMetrics": training,
        "jointTestMetrics": joint_metrics,
        "testSliceMetrics": slices,
        "visualAccuracyUplift": uplift,
        "developmentTestAccuracyGap": gap,
        "qualityGatesPassed": passed,
        "singleScoringModel": True,
        "visualClassifierUsedAsOfflineFeatureProducer": True,
        "productionEvidence": False,
        "promotionAllowed": False,
        "fallback": "contextual-recommender-action-context-v8-then-deterministic-ranking",
    }
    _write_json(result_path, result)
    opening = {
        "schemaVersion": 1,
        "status": "consumed",
        "testOpenBudget": 1,
        "testOpenCount": 1,
        "selectionUsedTest": False,
        "pretestLockSha256": _sha256(lock_path),
        "sealedTestSha256": expected["sealedTestSha256"],
        "resultSha256": _sha256(result_path),
        "reopenAllowed": False,
    }
    _write_json(opening_path, opening)
    return result


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage", choices=("select", "open-test"))
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[4]
    dataset = repo / "apps/demand-engine/evaluation/synthetic-marketplace-joint-scale-v9"
    policy = repo / "apps/demand-engine/policies/recommendation-joint-scale.v10.json"
    report = repo / "apps/demand-engine/evaluation/results/recommendation-joint-scale-development.v10.json"
    context = repo / "apps/demand-engine/models/contextual-scale-ablation.v10.linear.json"
    joint = repo / "apps/demand-engine/models/joint-context-visual-ranker.v10.linear.json"
    lock = dataset / "pretest-lock.v10.json"
    if args.stage == "select":
        value = select_models(dataset, policy, report, context, joint, lock)
        print(json.dumps({"context": value["contextAblation"]["trainingMetrics"], "joint": value["jointModel"]["trainingMetrics"], "uplift": value["developmentVisualAccuracyUplift"]}))
    else:
        value = open_test(
            dataset, policy, report, context, joint, lock,
            repo / "apps/demand-engine/evaluation/results/recommendation-joint-scale.v10.json",
            dataset / "test-opening-record.v10.json",
        )
        print(json.dumps(value))


if __name__ == "__main__":
    run()
