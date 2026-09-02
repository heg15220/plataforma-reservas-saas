"""Ablación baseline/multimodal con 5-fold y test pixel sellado."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

import numpy as np

from .recommendation_diverse_training import _metrics, _rolling_folds


class PairwiseLinearRanker:
    """Ranker lineal entrenado sobre diferencias positivo-negativo por consulta."""

    def __init__(self, coefficients: np.ndarray):
        self.coefficients = np.asarray(coefficients, dtype=np.float64)

    def predict(self, matrix: np.ndarray) -> np.ndarray:
        return np.asarray(matrix, dtype=np.float64) @ self.coefficients

    def save_model(self, path: Path) -> None:
        path.write_text(
            json.dumps(
                {
                    "schemaVersion": 1,
                    "algorithm": "pairwise-logistic-linear-ranker",
                    "coefficients": self.coefficients.tolist(),
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )

    @classmethod
    def load_model(cls, path: Path) -> "PairwiseLinearRanker":
        raw = json.loads(path.read_text(encoding="utf-8"))
        if raw.get("algorithm") != "pairwise-logistic-linear-ranker":
            raise ValueError("RECOMMENDATION_PIXEL_MODEL_FORMAT_INVALID")
        return cls(np.asarray(raw["coefficients"], dtype=np.float64))


def _fit_pairwise(
    parameters: dict[str, Any], sessions: list[dict[str, Any]], feature_names: list[str], seed: int
) -> PairwiseLinearRanker:
    """Ajusta utilidad relativa sin IDs, posición ni outcomes como features."""

    from sklearn.linear_model import LogisticRegression

    differences: list[list[float]] = []
    targets: list[int] = []
    for session in sessions:
        candidates = session["candidates"]
        positive = next(row for row in candidates if row["labels"]["relevance"] > 0)
        positive_values = np.asarray(
            [float(positive["features"][name]) for name in feature_names], dtype=np.float64
        )
        for negative in candidates:
            if negative is positive:
                continue
            negative_values = np.asarray(
                [float(negative["features"][name]) for name in feature_names], dtype=np.float64
            )
            delta = positive_values - negative_values
            differences.append(delta.tolist())
            targets.append(1)
            differences.append((-delta).tolist())
            targets.append(0)
    estimator = LogisticRegression(
        C=float(parameters["inverseRegularization"]),
        max_iter=int(parameters["maximumIterations"]),
        fit_intercept=False,
        solver="lbfgs",
        random_state=seed,
    )
    estimator.fit(np.asarray(differences, dtype=np.float64), np.asarray(targets, dtype=np.int32))
    return PairwiseLinearRanker(estimator.coef_[0])


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _policy(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if value.get("folds") != 5 or value.get("automaticPromotionAllowed") is not False:
        raise ValueError("RECOMMENDATION_PIXEL_POLICY_INVALID")
    if "pixelVisualAffinity" in value["baselineFeatureNames"]:
        raise ValueError("RECOMMENDATION_PIXEL_ABLATION_CONTAMINATED")
    if "pixelVisualAffinity" not in value["multimodalFeatureNames"]:
        raise ValueError("RECOMMENDATION_PIXEL_MULTIMODAL_FEATURE_MISSING")
    return value


def _select_variant(
    sessions: list[dict[str, Any]], policy: dict[str, Any], variant: str
) -> tuple[dict[str, Any], Any, dict[str, Any]]:
    features_key = "baselineFeatureNames" if variant == "baseline" else "multimodalFeatureNames"
    prior_key = "baselinePriorWeights" if variant == "baseline" else "multimodalPriorWeights"
    feature_names = policy[features_key]
    priors = policy[prior_key]
    reports = []
    folds = _rolling_folds(sessions, 5)
    for parameters in policy["hyperparameters"]:
        fold_metrics = []
        for train, validation in folds:
            model = _fit_pairwise(parameters, train, feature_names, policy["randomSeed"])
            fold_metrics.append(_metrics(model, validation, feature_names, priors, policy["topK"]))
        reports.append(
            {
                "parameters": parameters,
                "foldMetrics": fold_metrics,
                "meanAccuracy": round(float(np.mean([row["accuracy"] for row in fold_metrics])), 8),
                "meanPrecision": round(float(np.mean([row["precision"] for row in fold_metrics])), 8),
                "meanRecall": round(float(np.mean([row["recall"] for row in fold_metrics])), 8),
                "meanF1": round(float(np.mean([row["f1"] for row in fold_metrics])), 8),
                "meanRecallAtK": round(float(np.mean([row["recallAtK"] for row in fold_metrics])), 8),
            }
        )
    selected = max(reports, key=lambda row: (row["meanF1"], row["meanRecallAtK"], row["meanAccuracy"]))
    model = _fit_pairwise(selected["parameters"], sessions, feature_names, policy["randomSeed"])
    in_sample = _metrics(model, sessions, feature_names, priors, policy["topK"])
    summary = {
        "variant": variant,
        "featureNames": feature_names,
        "selectedParameters": selected["parameters"],
        "candidateReports": reports,
        "trainingMetrics": {
            "accuracy": selected["meanAccuracy"],
            "errorRate": round(1 - selected["meanAccuracy"], 8),
            "precision": selected["meanPrecision"],
            "recall": selected["meanRecall"],
            "f1": selected["meanF1"],
            "recallAtK": selected["meanRecallAtK"],
        },
        "inSampleDiagnostics": in_sample,
    }
    return summary, model, {"featureNames": feature_names, "priorWeights": priors}


def _scenario_candidate(feature_names: list[str], **updates: float) -> dict[str, Any]:
    values = {name: 0.25 for name in feature_names}
    values.update(updates)
    return {"venueId": f"scenario-{len(updates)}-{sum(values.values()):.6f}", "familyCode": "scenario", "features": values}


def pixel_scenarios(feature_names: list[str]) -> list[dict[str, Any]]:
    """Casos dirigidos para aporte visual, fallback y guardrails contextuales."""

    c = lambda **updates: _scenario_candidate(feature_names, **updates)
    return [
        {"code": "pixel-interest-breaks-context-tie", "expected": 0, "candidates": [
            c(contentAffinity=.8, taxonomyFamilyAffinity=1, pixelVisualAffinity=.95),
            c(contentAffinity=.8, taxonomyFamilyAffinity=1, pixelVisualAffinity=.05),
        ]},
        {"code": "explicit-visual-history-confidence", "expected": 0, "candidates": [
            c(contentAffinity=.8, pixelVisualAffinity=.9, pixelVisualHistoryConfidence=1),
            c(contentAffinity=.8, pixelVisualAffinity=.3, pixelVisualHistoryConfidence=1),
        ]},
        {"code": "no-history-contextual-fallback", "expected": 0, "candidates": [
            c(contentAffinity=.95, serviceAffinity=1, pixelVisualAffinity=0, pixelVisualHistoryConfidence=0),
            c(contentAffinity=.2, serviceAffinity=0, pixelVisualAffinity=0, pixelVisualHistoryConfidence=0),
        ]},
        {"code": "visual-cannot-override-incompatible-type", "expected": 0, "candidates": [
            c(taxonomyTypeAffinity=1, taxonomyFamilyAffinity=1, serviceAffinity=1, contentAffinity=1, pixelVisualAffinity=.6),
            c(taxonomyTypeAffinity=0, taxonomyFamilyAffinity=0, serviceAffinity=0, contentAffinity=.05, pixelVisualAffinity=1),
        ]},
        {"code": "aligned-scarce-low-exposure", "expected": 0, "candidates": [
            c(contentAffinity=.95, serviceAffinity=1, pixelVisualAffinity=.9, availabilityRatio=.12,
              alignedScarcityOpportunity=.85, lowExposureAffinity=.95, capacityOpportunity=.82),
            c(contentAffinity=.2, qualityScore=1, availabilityRatio=.95, pixelVisualAffinity=.15),
        ]},
        {"code": "same-type-visual-hard-negative", "expected": 0, "candidates": [
            c(taxonomyTypeAffinity=1, contentAffinity=.9, pixelVisualAffinity=.92),
            c(taxonomyTypeAffinity=1, contentAffinity=.9, pixelVisualAffinity=.1),
        ]},
        {"code": "cold-venue-with-matching-image", "expected": 0, "candidates": [
            c(isNewVenue=1, contentAffinity=.85, pixelVisualAffinity=.95),
            c(isNewVenue=0, contentAffinity=.85, pixelVisualAffinity=.2, qualityScore=.9),
        ]},
        {"code": "location-still-breaks-visual-tie", "expected": 0, "candidates": [
            c(contentAffinity=.8, pixelVisualAffinity=.8, proximity=.95),
            c(contentAffinity=.8, pixelVisualAffinity=.8, proximity=.05),
        ]},
    ]


def _scenario_metrics(model: Any, feature_names: list[str], priors: dict[str, float]) -> dict[str, Any]:
    results = []
    for scenario in pixel_scenarios(feature_names):
        candidates = scenario["candidates"]
        matrix = np.asarray([[row["features"][name] for name in feature_names] for row in candidates], dtype=np.float64)
        scores = np.asarray(model.predict(matrix), dtype=np.float64)
        for name, weight in priors.items():
            scores += float(weight) * matrix[:, feature_names.index(name)]
        predicted = int(np.argmax(scores))
        results.append({"scenarioCode": scenario["code"], "passed": predicted == scenario["expected"]})
    passed = sum(row["passed"] for row in results)
    accuracy = passed / len(results)
    return {
        "cases": len(results), "passed": passed, "accuracy": round(accuracy, 8),
        "errorRate": round(1 - accuracy, 8), "precision": round(accuracy, 8),
        "recall": round(accuracy, 8), "f1": round(accuracy, 8), "results": results,
    }


def select_models(
    dataset_root: Path, policy_path: Path, report_path: Path,
    baseline_model_path: Path, multimodal_model_path: Path, lock_path: Path,
) -> dict[str, Any]:
    """Selecciona ambos brazos exclusivamente con desarrollo y congela el test."""

    policy = _policy(policy_path)
    development_path = dataset_root / "development-sessions.jsonl"
    sessions = _read_jsonl(development_path)
    if {row["split"] for row in sessions} != {"train", "validation"}:
        raise ValueError("RECOMMENDATION_PIXEL_DEVELOPMENT_SPLIT_INVALID")
    baseline, baseline_model, _ = _select_variant(sessions, policy, "baseline")
    multimodal, multimodal_model, multi_contract = _select_variant(sessions, policy, "multimodal")
    baseline_model_path.parent.mkdir(parents=True, exist_ok=True)
    baseline_model.save_model(baseline_model_path)
    multimodal_model.save_model(multimodal_model_path)
    scenarios = _scenario_metrics(multimodal_model, multi_contract["featureNames"], multi_contract["priorWeights"])
    uplift = round(multimodal["trainingMetrics"]["accuracy"] - baseline["trainingMetrics"]["accuracy"], 8)
    report = {
        "schemaVersion": 4,
        "reportVersion": "recommendation-pixel-development-v4",
        "datasetVersion": policy["datasetVersion"],
        "foldStrategy": "five-fold-rolling-origin",
        "folds": 5,
        "baseline": baseline,
        "multimodal": multimodal,
        "developmentVisualAccuracyUplift": uplift,
        "pixelScenarioMetrics": scenarios,
        "testOpened": False,
        "promotionAllowed": False,
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lock = {
        "schemaVersion": 1,
        "status": "baseline-and-multimodal-selected-test-sealed",
        "developmentSessionsSha256": _sha(development_path),
        "sealedTestSha256": _sha(dataset_root / "test-sessions.sealed.jsonl"),
        "visualLinkageSha256": _sha(dataset_root / "visual-linkage.jsonl"),
        "onboardingEventsSha256": _sha(dataset_root / "visual-onboarding-events.jsonl"),
        "policySha256": _sha(policy_path),
        "developmentReportSha256": _sha(report_path),
        "baselineModelSha256": _sha(baseline_model_path),
        "multimodalModelSha256": _sha(multimodal_model_path),
        "testOpenBudget": 1,
        "testOpenCount": 0,
    }
    lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def open_test(
    dataset_root: Path, policy_path: Path, development_report_path: Path,
    baseline_model_path: Path, multimodal_model_path: Path, lock_path: Path,
    result_path: Path, opening_record_path: Path,
) -> dict[str, Any]:
    """Evalúa una vez ambos brazos sobre exactamente las mismas sesiones selladas."""

    if result_path.exists() or opening_record_path.exists():
        raise ValueError("RECOMMENDATION_PIXEL_TEST_ALREADY_OPENED")
    policy = _policy(policy_path)
    lock = json.loads(lock_path.read_text(encoding="utf-8"))
    expected = {
        "developmentSessionsSha256": _sha(dataset_root / "development-sessions.jsonl"),
        "sealedTestSha256": _sha(dataset_root / "test-sessions.sealed.jsonl"),
        "visualLinkageSha256": _sha(dataset_root / "visual-linkage.jsonl"),
        "onboardingEventsSha256": _sha(dataset_root / "visual-onboarding-events.jsonl"),
        "policySha256": _sha(policy_path),
        "developmentReportSha256": _sha(development_report_path),
        "baselineModelSha256": _sha(baseline_model_path),
        "multimodalModelSha256": _sha(multimodal_model_path),
    }
    if lock.get("status") != "baseline-and-multimodal-selected-test-sealed" or any(lock.get(key) != value for key, value in expected.items()):
        raise ValueError("RECOMMENDATION_PIXEL_PRETEST_HASH_MISMATCH")
    baseline_model = PairwiseLinearRanker.load_model(baseline_model_path)
    multimodal_model = PairwiseLinearRanker.load_model(multimodal_model_path)
    test = _read_jsonl(dataset_root / "test-sessions.sealed.jsonl")
    baseline = _metrics(
        baseline_model, test, policy["baselineFeatureNames"], policy["baselinePriorWeights"], policy["topK"]
    )
    multimodal = _metrics(
        multimodal_model, test, policy["multimodalFeatureNames"], policy["multimodalPriorWeights"], policy["topK"]
    )
    development = json.loads(development_report_path.read_text(encoding="utf-8"))
    training = development["multimodal"]["trainingMetrics"]
    uplift = round(multimodal["accuracy"] - baseline["accuracy"], 8)
    gap = round(abs(training["accuracy"] - multimodal["accuracy"]), 8)
    gates = policy["gates"]
    passed = (
        training["accuracy"] <= gates["maximumTrainingAccuracy"]
        and multimodal["accuracy"] >= gates["minimumTestAccuracy"]
        and multimodal["errorRate"] < gates["maximumTestErrorExclusive"]
        and multimodal["precision"] >= gates["minimumTestPrecision"]
        and multimodal["recall"] >= gates["minimumTestRecall"]
        and multimodal["f1"] >= gates["minimumTestF1"]
        and uplift >= gates["minimumVisualAccuracyUplift"]
        and development["pixelScenarioMetrics"]["accuracy"] >= gates["minimumMultimodalScenarioAccuracy"]
        and gap <= gates["maximumTrainTestGap"]
    )
    result = {
        "schemaVersion": 4,
        "reportVersion": "recommendation-pixel-personalization-v4",
        "datasetVersion": policy["datasetVersion"],
        "folds": 5,
        "foldStrategy": "five-fold-rolling-origin",
        "baselineTestMetrics": baseline,
        "multimodalTrainingMetrics": training,
        "multimodalTestMetrics": multimodal,
        "pixelScenarioMetrics": development["pixelScenarioMetrics"],
        "testVisualAccuracyUplift": uplift,
        "multimodalTrainTestAccuracyGap": gap,
        "qualityGatesPassed": passed,
        "pixelPatternsUsed": True,
        "pixelMethod": "cosine similarity between frozen CLIP image embeddings and point-in-time user visual profile",
        "productionEvidence": False,
        "promotionAllowed": False,
        "fallback": "contextual-model-without-vision-then-deterministic-ranking",
    }
    result_path.parent.mkdir(parents=True, exist_ok=True)
    result_path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    opening = {
        "schemaVersion": 1,
        "status": "consumed",
        "testOpenBudget": 1,
        "testOpenCount": 1,
        "selectionUsedTest": False,
        "pretestLockSha256": _sha(lock_path),
        "sealedTestSha256": expected["sealedTestSha256"],
        "resultSha256": _sha(result_path),
    }
    opening_record_path.write_text(json.dumps(opening, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return result


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage", choices=("select", "open-test"))
    parser.add_argument("--dataset-root", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--development-report", type=Path, required=True)
    parser.add_argument("--baseline-model", type=Path, required=True)
    parser.add_argument("--multimodal-model", type=Path, required=True)
    parser.add_argument("--lock", type=Path, required=True)
    parser.add_argument("--result", type=Path)
    parser.add_argument("--opening-record", type=Path)
    args = parser.parse_args()
    if args.stage == "select":
        report = select_models(
            args.dataset_root, args.policy, args.development_report,
            args.baseline_model, args.multimodal_model, args.lock,
        )
        print(json.dumps({
            "baseline": report["baseline"]["trainingMetrics"],
            "multimodal": report["multimodal"]["trainingMetrics"],
            "uplift": report["developmentVisualAccuracyUplift"], "testOpened": False,
        }))
    else:
        if args.result is None or args.opening_record is None:
            parser.error("open-test requiere --result y --opening-record")
        report = open_test(
            args.dataset_root, args.policy, args.development_report,
            args.baseline_model, args.multimodal_model, args.lock,
            args.result, args.opening_record,
        )
        print(json.dumps(report))


if __name__ == "__main__":
    run()
