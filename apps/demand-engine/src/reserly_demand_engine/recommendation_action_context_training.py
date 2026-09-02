"""Selección 5-fold y test único del recomendador contextual por acciones v5."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any, Callable

import numpy as np

from .recommendation_diverse_training import _fit, _matrix, _metrics, _read_jsonl, _rolling_folds, _scores


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _policy(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if value.get("folds") != 5 or value.get("automaticPromotionAllowed") is not False:
        raise ValueError("RECOMMENDATION_ACTION_CONTEXT_POLICY_INVALID")
    forbidden = {"venueId", "profileId", "position", "clicked", "bookingCompleted", "relevance", "latitude", "longitude"}
    if forbidden.intersection(value["featureNames"]):
        raise ValueError("RECOMMENDATION_ACTION_CONTEXT_LEAKAGE_FEATURE")
    return value


def behavioral_scenarios(feature_names: list[str]) -> list[dict[str, Any]]:
    """Contrafactuales: solo cambia la señal cuya conducta se quiere probar."""

    def candidate(code: str, **updates: float) -> dict[str, Any]:
        values = {name: .2 for name in feature_names}
        values.update(updates)
        return {"venueId": code, "familyCode": "scenario", "features": values}

    aligned = dict(recentActionTypeAffinity=1, recentActionFamilyAffinity=1, recentActionServiceAffinity=1,
                   actionSequenceMomentum=1, taxonomyTypeAffinity=1, taxonomyFamilyAffinity=1,
                   serviceAffinity=1, contentAffinity=1, withinPreferredRadius=1)
    return [
        {"code": "recent-actions-select-current-category", "expected": 0, "candidates": [
            candidate("current", **aligned),
            candidate("historic", persistentPreferenceAffinity=1, qualityScore=1, contentAffinity=.2),
        ]},
        {"code": "location-breaks-equal-intent-tie", "expected": 0, "candidates": [
            candidate("near", **aligned, currentLocationProximity=1, distanceDecayKm=1),
            candidate("far", **aligned, currentLocationProximity=.02, distanceDecayKm=.02),
        ]},
        {"code": "location-cannot-override-category-mismatch", "expected": 0, "candidates": [
            candidate("aligned-near-enough", **aligned, currentLocationProximity=.55, distanceDecayKm=.55),
            candidate("wrong-next-door", currentLocationProximity=1, distanceDecayKm=1, qualityScore=1, contentAffinity=.05),
        ]},
        {"code": "scarce-slot-for-aligned-near-user", "expected": 0, "candidates": [
            candidate("scarce", **aligned, currentLocationProximity=.9, distanceDecayKm=.9, availabilityRatio=.1,
                      remainingSlotUrgency=.9, alignedScarcityOpportunity=.81, lowExposureAffinity=.9),
            candidate("available", **aligned, currentLocationProximity=.9, distanceDecayKm=.9, availabilityRatio=.8,
                      remainingSlotUrgency=.2, alignedScarcityOpportunity=.18),
        ]},
        {"code": "scarcity-never-overrides-mismatch", "expected": 0, "candidates": [
            candidate("aligned", **aligned, currentLocationProximity=.65, distanceDecayKm=.65, availabilityRatio=.7),
            candidate("wrong-scarce", currentLocationProximity=1, distanceDecayKm=1, remainingSlotUrgency=1,
                      alignedScarcityOpportunity=0, qualityScore=1, contentAffinity=.03),
        ]},
        {"code": "requested-hour", "expected": 0, "candidates": [
            candidate("right-hour", **aligned, requestedHourAffinity=1),
            candidate("wrong-hour", **aligned, requestedHourAffinity=.1),
        ]},
        {"code": "requested-day", "expected": 0, "candidates": [
            candidate("right-day", **aligned, requestedDayAffinity=1),
            candidate("wrong-day", **aligned, requestedDayAffinity=.1),
        ]},
        {"code": "service-specialty", "expected": 0, "candidates": [
            candidate("service", **aligned),
            candidate("same-family", recentActionFamilyAffinity=1, taxonomyFamilyAffinity=1, contentAffinity=.45, qualityScore=1),
        ]},
        {"code": "consented-preference-breaks-content-tie", "expected": 0, "candidates": [
            candidate("preferred", **aligned, persistentPreferenceAffinity=1),
            candidate("neutral", **aligned, persistentPreferenceAffinity=0),
        ]},
        {"code": "underexposed-compatible-local", "expected": 0, "candidates": [
            candidate("underexposed", **aligned, lowExposureAffinity=1),
            candidate("popular", **aligned, lowExposureAffinity=0),
        ]},
    ]


def _scenario_metrics(model: Any, policy: dict[str, Any]) -> dict[str, Any]:
    results = []
    for scenario in behavioral_scenarios(policy["featureNames"]):
        scores = _scores(model, scenario["candidates"], policy["featureNames"], policy["businessPriorWeights"])
        predicted = int(np.argmax(scores))
        results.append({"scenarioCode": scenario["code"], "passed": predicted == scenario["expected"]})
    passed = sum(row["passed"] for row in results)
    accuracy = passed / len(results)
    return {"cases": len(results), "passed": passed, "accuracy": round(accuracy, 8), "errorRate": round(1 - accuracy, 8),
            "precision": round(accuracy, 8), "recall": round(accuracy, 8), "f1": round(accuracy, 8), "results": results}


def _slice_metrics(model: Any, sessions: list[dict[str, Any]], policy: dict[str, Any]) -> dict[str, Any]:
    predicates: dict[str, Callable[[dict[str, Any], dict[str, Any]], bool]] = {
        "scarceAligned": lambda session, actual: actual["features"]["alignedScarcityOpportunity"] >= .45,
        "locationSensitive": lambda session, actual: max(row["distanceKm"] for row in session["candidates"]) - actual["distanceKm"] >= 5,
        "intentPivot": lambda session, actual: any(action.get("typeCode") == "previous-intent" for action in session["actionHistory"]),
        "evening": lambda session, actual: session["requestedSlot"]["hour"] >= 17,
        "coldVenue": lambda session, actual: actual["features"]["isNewVenue"] == 1,
    }
    result: dict[str, Any] = {}
    for name, predicate in predicates.items():
        selected = []
        for session in sessions:
            actual = next(row for row in session["candidates"] if row["labels"]["relevance"] > 0)
            if predicate(session, actual):
                selected.append(session)
        metrics = _metrics(model, selected, policy["featureNames"], policy["businessPriorWeights"], policy["topK"])
        result[name] = {"sessions": metrics["sessions"], "accuracy": metrics["accuracy"], "f1": metrics["f1"]}
    return result


def select_model(dataset_root: Path, policy_path: Path, report_path: Path, model_path: Path, lock_path: Path) -> dict[str, Any]:
    """Selecciona sin leer el holdout y sella todos los hashes relevantes."""

    policy = _policy(policy_path)
    development_path = dataset_root / "development-sessions.jsonl"
    sessions = _read_jsonl(development_path)
    folds = _rolling_folds(sessions, policy["folds"])
    reports = []
    for parameters in policy["hyperparameters"]:
        fold_metrics = []
        for train, validation in folds:
            x, y, groups = _matrix(train, policy["featureNames"])
            model = _fit(parameters, x, y, groups, policy["randomSeed"])
            fold_metrics.append(_metrics(model, validation, policy["featureNames"], policy["businessPriorWeights"], policy["topK"]))
        reports.append({"parameters": parameters, "foldMetrics": fold_metrics,
                        "meanAccuracy": round(float(np.mean([row["accuracy"] for row in fold_metrics])), 8),
                        "meanPrecision": round(float(np.mean([row["precision"] for row in fold_metrics])), 8),
                        "meanRecall": round(float(np.mean([row["recall"] for row in fold_metrics])), 8),
                        "meanF1": round(float(np.mean([row["f1"] for row in fold_metrics])), 8),
                        "meanRecallAtK": round(float(np.mean([row["recallAtK"] for row in fold_metrics])), 8)})
    selected = max(reports, key=lambda row: (row["meanF1"], row["meanRecallAtK"]))
    x, y, groups = _matrix(sessions, policy["featureNames"])
    model = _fit(selected["parameters"], x, y, groups, policy["randomSeed"])
    model_path.parent.mkdir(parents=True, exist_ok=True)
    model.save_model(model_path)
    report = {
        "schemaVersion": policy["schemaVersion"], "reportVersion": policy.get("developmentReportVersion", "recommendation-action-context-development-v5"),
        "datasetVersion": policy["datasetVersion"], "foldStrategy": "five-fold-rolling-origin", "folds": 5,
        "selectedParameters": selected["parameters"], "candidateReports": reports,
        "trainingMetricsDefinition": "mean rolling-origin out-of-fold development metrics",
        "trainingMetrics": {"accuracy": selected["meanAccuracy"], "errorRate": round(1 - selected["meanAccuracy"], 8),
                            "precision": selected["meanPrecision"], "recall": selected["meanRecall"],
                            "f1": selected["meanF1"], "recallAtK": selected["meanRecallAtK"]},
        "inSampleDiagnostics": _metrics(model, sessions, policy["featureNames"], policy["businessPriorWeights"], policy["topK"]),
        "behavioralScenarioMetrics": _scenario_metrics(model, policy), "developmentSliceMetrics": _slice_metrics(model, sessions, policy),
        "testOpened": False, "promotionAllowed": False,
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lock = {"schemaVersion": 1, "status": "model-selected-test-sealed", "developmentSessionsSha256": _sha(development_path),
            "sealedTestSha256": _sha(dataset_root / "test-sessions.sealed.jsonl"), "policySha256": _sha(policy_path),
            "developmentReportSha256": _sha(report_path), "modelSha256": _sha(model_path), "testOpenBudget": 1, "testOpenCount": 0}
    lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def open_test(dataset_root: Path, policy_path: Path, development_report_path: Path, model_path: Path,
              lock_path: Path, result_path: Path, opening_record_path: Path) -> dict[str, Any]:
    """Consume el presupuesto del holdout una sola vez y nunca ajusta el modelo."""

    if result_path.exists() or opening_record_path.exists():
        raise ValueError("RECOMMENDATION_ACTION_CONTEXT_TEST_ALREADY_OPENED")
    policy = _policy(policy_path)
    lock = json.loads(lock_path.read_text(encoding="utf-8"))
    expected = {"developmentSessionsSha256": _sha(dataset_root / "development-sessions.jsonl"),
                "sealedTestSha256": _sha(dataset_root / "test-sessions.sealed.jsonl"), "policySha256": _sha(policy_path),
                "developmentReportSha256": _sha(development_report_path), "modelSha256": _sha(model_path)}
    if lock.get("status") != "model-selected-test-sealed" or any(lock.get(key) != value for key, value in expected.items()):
        raise ValueError("RECOMMENDATION_ACTION_CONTEXT_PRETEST_HASH_MISMATCH")
    from xgboost import XGBRanker
    model = XGBRanker()
    model.load_model(model_path)
    test = _read_jsonl(dataset_root / "test-sessions.sealed.jsonl")
    test_metrics = _metrics(model, test, policy["featureNames"], policy["businessPriorWeights"], policy["topK"])
    development = json.loads(development_report_path.read_text(encoding="utf-8"))
    training = development["trainingMetrics"]
    gap = round(abs(training["accuracy"] - test_metrics["accuracy"]), 8)
    gates = policy["gates"]
    scenarios = development["behavioralScenarioMetrics"]
    slices = _slice_metrics(model, test, policy)
    passed = (training["accuracy"] <= gates["maximumTrainingAccuracy"] and test_metrics["accuracy"] >= gates["minimumTestAccuracy"]
              and test_metrics["errorRate"] < gates["maximumTestErrorExclusive"] and test_metrics["precision"] >= gates["minimumTestPrecision"]
              and test_metrics["recall"] >= gates["minimumTestRecall"] and test_metrics["f1"] >= gates["minimumTestF1"]
              and test_metrics["macroFamilyF1"] >= gates["minimumMacroFamilyMetric"] and gap <= gates["maximumTrainTestGap"]
              and scenarios["accuracy"] >= gates["minimumBehavioralScenarioAccuracy"]
              and all(row["sessions"] == 0 or row["accuracy"] >= gates["minimumSliceAccuracy"] for row in slices.values()))
    result = {"schemaVersion": policy["schemaVersion"], "reportVersion": policy.get("reportVersion", "recommendation-action-context-v5"), "modelVersion": policy["modelVersion"],
              "modelSha256": _sha(model_path), "datasetVersion": policy["datasetVersion"], "folds": 5,
              "foldStrategy": "five-fold-rolling-origin", "trainingMetrics": training, "testMetrics": test_metrics,
              "behavioralScenarioMetrics": scenarios, "testSliceMetrics": slices, "trainTestAccuracyGap": gap,
              "qualityGatesPassed": passed, "locationSignalRequired": True, "scarcityIsConditional": True,
              "productionEvidence": False, "promotionAllowed": False, "fallback": "contextual-v4-then-deterministic-ranking",
              "limitations": ["Evaluación sintética; requiere shadow y A/B con datos reales.", "La ubicación de producción exige consentimiento, TTL y precisión minimizada."]}
    result_path.parent.mkdir(parents=True, exist_ok=True)
    result_path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    opening_record_path.write_text(json.dumps({"schemaVersion": 1, "status": "consumed", "testOpenBudget": 1, "testOpenCount": 1,
                                               "pretestLockSha256": _sha(lock_path), "sealedTestSha256": expected["sealedTestSha256"],
                                               "resultSha256": _sha(result_path), "selectionUsedTest": False}, indent=2) + "\n", encoding="utf-8")
    return result


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage", choices=("select", "open-test")); parser.add_argument("--dataset-root", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True); parser.add_argument("--development-report", type=Path, required=True)
    parser.add_argument("--model", type=Path, required=True); parser.add_argument("--lock", type=Path, required=True)
    parser.add_argument("--result", type=Path); parser.add_argument("--opening-record", type=Path)
    args = parser.parse_args()
    if args.stage == "select":
        value = select_model(args.dataset_root, args.policy, args.development_report, args.model, args.lock)
    else:
        if not args.result or not args.opening_record: parser.error("open-test requiere rutas de resultado")
        value = open_test(args.dataset_root, args.policy, args.development_report, args.model, args.lock, args.result, args.opening_record)
    print(json.dumps(value.get("testMetrics", value["trainingMetrics"])))


if __name__ == "__main__":
    run()
