"""Selección 5-fold y apertura única del test del recomendador diverso v2.

`select` solo lee desarrollo, elige hiperparámetros por rolling-origin, entrena
el artefacto y congela hashes. `open-test` verifica esos hashes y bloquea una
segunda apertura. El modelo es consultivo: nunca elude elegibilidad, capacidad
ni los fallbacks deterministas del producto.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

import numpy as np


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _time(row: dict[str, Any]) -> datetime:
    return datetime.fromisoformat(row["occurredAt"].replace("Z", "+00:00"))


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _rolling_folds(
    sessions: list[dict[str, Any]], folds: int
) -> list[tuple[list[dict[str, Any]], list[dict[str, Any]]]]:
    """Crea folds expansivos donde toda validación es posterior a su train."""

    ordered = sorted(sessions, key=lambda row: (_time(row), row["sessionId"]))
    boundaries = [round(len(ordered) * index / (folds + 1)) for index in range(folds + 2)]
    result = []
    for fold in range(folds):
        train = ordered[: boundaries[fold + 1]]
        validation = ordered[boundaries[fold + 1] : boundaries[fold + 2]]
        if not train or not validation or _time(train[-1]) > _time(validation[0]):
            raise ValueError("RECOMMENDATION_DIVERSE_TEMPORAL_FOLD_INVALID")
        result.append((train, validation))
    return result


def _matrix(
    sessions: list[dict[str, Any]], feature_names: list[str]
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    values: list[list[float]] = []
    labels: list[int] = []
    groups: list[int] = []
    for session in sorted(sessions, key=lambda row: (_time(row), row["sessionId"])):
        candidates = session["candidates"]
        if (
            len(candidates) != 8
            or sum(candidate["labels"]["relevance"] > 0 for candidate in candidates) != 1
            or any(not candidate["eligible"] or not candidate["capacityAvailable"] for candidate in candidates)
        ):
            raise ValueError("RECOMMENDATION_DIVERSE_CANDIDATE_CONTRACT_INVALID")
        groups.append(len(candidates))
        for candidate in candidates:
            values.append([float(candidate["features"][name]) for name in feature_names])
            labels.append(int(candidate["labels"]["relevance"]))
    return (
        np.asarray(values, dtype=np.float64),
        np.asarray(labels, dtype=np.int32),
        np.asarray(groups, dtype=np.uint32),
    )


def _fit(
    parameters: dict[str, Any], features: np.ndarray, labels: np.ndarray,
    groups: np.ndarray, seed: int,
) -> Any:
    """Entrena LambdaMART regularizado con ejecución determinista de un hilo."""

    from xgboost import XGBRanker

    model = XGBRanker(
        objective="rank:ndcg",
        eval_metric="ndcg@3",
        n_estimators=parameters["nEstimators"],
        max_depth=parameters["maximumDepth"],
        learning_rate=parameters["learningRate"],
        min_child_weight=parameters["minimumChildWeight"],
        reg_lambda=parameters["l2Penalty"],
        reg_alpha=parameters["l1Penalty"],
        subsample=parameters["subsample"],
        colsample_bytree=parameters["columnSample"],
        random_state=seed,
        n_jobs=1,
        tree_method="hist",
        verbosity=0,
    )
    model.fit(features, labels, group=groups, verbose=False)
    return model


def _scores(
    model: Any, candidates: list[dict[str, Any]], feature_names: list[str],
    prior_weights: dict[str, float],
) -> np.ndarray:
    matrix = np.asarray(
        [[float(candidate["features"][name]) for name in feature_names] for candidate in candidates],
        dtype=np.float64,
    )
    scores = np.asarray(model.predict(matrix), dtype=np.float64)
    for name, weight in prior_weights.items():
        scores += float(weight) * matrix[:, feature_names.index(name)]
    return scores


def _metrics(
    model: Any, sessions: list[dict[str, Any]], feature_names: list[str],
    prior_weights: dict[str, float], top_k: int,
) -> dict[str, Any]:
    hits = 0
    top_k_hits = 0
    actual_family: Counter[str] = Counter()
    predicted_family: Counter[str] = Counter()
    correct_family: Counter[str] = Counter()
    ambiguous_hits = clear_hits = ambiguous_total = clear_total = 0
    for session in sessions:
        candidates = session["candidates"]
        actual = next(index for index, row in enumerate(candidates) if row["labels"]["relevance"] > 0)
        scores = _scores(model, candidates, feature_names, prior_weights)
        order = sorted(
            range(len(candidates)),
            key=lambda index: (-float(scores[index]), candidates[index]["venueId"]),
        )
        predicted = order[0]
        hit = predicted == actual
        hits += int(hit)
        top_k_hits += int(actual in order[:top_k])
        actual_code = candidates[actual]["familyCode"]
        predicted_code = candidates[predicted]["familyCode"]
        actual_family[actual_code] += 1
        predicted_family[predicted_code] += 1
        correct_family[actual_code] += int(actual_code == predicted_code)
        if session["ambiguousObservedChoice"]:
            ambiguous_total += 1
            ambiguous_hits += int(hit)
        else:
            clear_total += 1
            clear_hits += int(hit)
    total = len(sessions)
    accuracy = hits / total if total else 0.0
    family_codes = sorted(set(actual_family) | set(predicted_family))
    family_precision = [
        correct_family[code] / predicted_family[code] if predicted_family[code] else 0.0
        for code in family_codes
    ]
    family_recall = [correct_family[code] / actual_family[code] for code in family_codes]
    macro_precision = float(np.mean(family_precision)) if family_precision else 0.0
    macro_recall = float(np.mean(family_recall)) if family_recall else 0.0
    macro_f1_by_class = [
        2 * precision * recall / (precision + recall) if precision + recall else 0.0
        for precision, recall in zip(family_precision, family_recall, strict=True)
    ]
    return {
        "sessions": total,
        "correctTop1": hits,
        "accuracy": round(accuracy, 8),
        "errorRate": round(1.0 - accuracy, 8),
        # Una predicción y un positivo por consulta: precision/recall/F1 top-1
        # coinciden matemáticamente con la tasa de acierto, sin siete TN inflados.
        "precision": round(accuracy, 8),
        "recall": round(accuracy, 8),
        "f1": round(accuracy, 8),
        "precisionAtK": round(top_k_hits / (total * top_k), 8) if total else 0.0,
        "recallAtK": round(top_k_hits / total, 8) if total else 0.0,
        "macroFamilyPrecision": round(macro_precision, 8),
        "macroFamilyRecall": round(macro_recall, 8),
        "macroFamilyF1": round(float(np.mean(macro_f1_by_class)), 8) if macro_f1_by_class else 0.0,
        "clearChoiceAccuracy": round(clear_hits / clear_total, 8) if clear_total else 0.0,
        "ambiguousChoiceAccuracy": round(ambiguous_hits / ambiguous_total, 8) if ambiguous_total else 0.0,
        "ambiguousSessions": ambiguous_total,
    }


def business_scenarios(feature_names: list[str]) -> list[dict[str, Any]]:
    """Contratos contrafactuales que representan recorridos diversos solicitados."""

    def candidate(**updates: float) -> dict[str, Any]:
        values = {name: 0.25 for name in feature_names}
        values.update(updates)
        return {
            "venueId": f"scenario-{len(updates)}-{sum(values.values()):.4f}",
            "familyCode": "scenario-family",
            "features": values,
        }

    return [
        {"code": "aligned-scarce-underexposed", "expected": 0, "candidates": [
            candidate(taxonomyTypeAffinity=1, taxonomyFamilyAffinity=1, contentAffinity=1,
                      serviceAffinity=1, availabilityRatio=.12, alignedScarcityOpportunity=.88,
                      lowExposureAffinity=.95, capacityOpportunity=.84, qualityScore=.52),
            candidate(taxonomyTypeAffinity=0, taxonomyFamilyAffinity=0, contentAffinity=.22,
                      qualityScore=.98, availabilityRatio=.92, lowExposureAffinity=.02),
        ]},
        {"code": "visual-ambience", "expected": 0, "candidates": [
            candidate(contentAffinity=.82, visualAmbienceAffinity=1),
            candidate(contentAffinity=.82, visualAmbienceAffinity=0),
        ]},
        {"code": "common-hour", "expected": 0, "candidates": [
            candidate(contentAffinity=.82, commonHourAffinity=1),
            candidate(contentAffinity=.82, commonHourAffinity=0),
        ]},
        {"code": "nearby-compatible", "expected": 0, "candidates": [
            candidate(contentAffinity=.88, proximity=1), candidate(contentAffinity=.88, proximity=.05),
        ]},
        {"code": "specialty-type", "expected": 0, "candidates": [
            candidate(taxonomyTypeAffinity=1, taxonomyFamilyAffinity=1, serviceAffinity=1, contentAffinity=1),
            candidate(taxonomyTypeAffinity=0, taxonomyFamilyAffinity=1, serviceAffinity=0, contentAffinity=.2, qualityScore=1),
        ]},
        {"code": "attribute-match", "expected": 0, "candidates": [
            candidate(contentAffinity=.82, attributeAffinity=1), candidate(contentAffinity=.82, attributeAffinity=0),
        ]},
        {"code": "cold-start", "expected": 0, "candidates": [
            candidate(contentAffinity=1, serviceAffinity=1, isNewVenue=1, lowExposureAffinity=1),
            candidate(contentAffinity=.35, isNewVenue=0, qualityScore=.95),
        ]},
        {"code": "quality-does-not-override-intent", "expected": 0, "candidates": [
            candidate(contentAffinity=1, serviceAffinity=1, qualityScore=.5),
            candidate(contentAffinity=.15, serviceAffinity=0, qualityScore=1),
        ]},
        {"code": "capacity", "expected": 0, "candidates": [
            candidate(contentAffinity=.9, availabilityRatio=.8), candidate(contentAffinity=.9, availabilityRatio=.05),
        ]},
        {"code": "price-distance", "expected": 0, "candidates": [
            candidate(contentAffinity=.82, priceFit=1, proximity=1),
            candidate(contentAffinity=.82, priceFit=0, proximity=.05),
        ]},
        {"code": "same-family-hard-negative", "expected": 0, "candidates": [
            candidate(taxonomyFamilyAffinity=1, taxonomyTypeAffinity=1, contentAffinity=1),
            candidate(taxonomyFamilyAffinity=1, taxonomyTypeAffinity=0, contentAffinity=.65),
        ]},
        {"code": "visual-cannot-override-type", "expected": 0, "candidates": [
            candidate(taxonomyTypeAffinity=1, contentAffinity=.9, visualAmbienceAffinity=.3),
            candidate(taxonomyTypeAffinity=0, contentAffinity=.25, visualAmbienceAffinity=1),
        ]},
    ]


def _scenario_metrics(
    model: Any, feature_names: list[str], prior_weights: dict[str, float]
) -> dict[str, Any]:
    results = []
    for scenario in business_scenarios(feature_names):
        scores = _scores(model, scenario["candidates"], feature_names, prior_weights)
        predicted = int(np.argmax(scores))
        results.append({"scenarioCode": scenario["code"], "passed": predicted == scenario["expected"]})
    passed = sum(row["passed"] for row in results)
    accuracy = passed / len(results)
    return {
        "cases": len(results), "passed": passed, "accuracy": round(accuracy, 8),
        "errorRate": round(1 - accuracy, 8), "precision": round(accuracy, 8),
        "recall": round(accuracy, 8), "f1": round(accuracy, 8), "results": results,
    }


def _load_policy(policy_path: Path) -> dict[str, Any]:
    policy = json.loads(policy_path.read_text(encoding="utf-8"))
    if policy.get("folds") != 5 or policy.get("automaticPromotionAllowed") is not False:
        raise ValueError("RECOMMENDATION_DIVERSE_POLICY_INVALID")
    return policy


def select_model(
    dataset_root: Path, policy_path: Path, report_path: Path,
    model_path: Path, lock_path: Path,
) -> dict[str, Any]:
    """Selecciona solo con desarrollo y congela artefactos antes del test."""

    policy = _load_policy(policy_path)
    development_path = dataset_root / "development-sessions.jsonl"
    sessions = _read_jsonl(development_path)
    if {row["split"] for row in sessions} != {"train", "validation"}:
        raise ValueError("RECOMMENDATION_DIVERSE_DEVELOPMENT_SPLITS_INVALID")
    folds = _rolling_folds(sessions, policy["folds"])
    candidates = []
    for parameters in policy["hyperparameters"]:
        fold_metrics = []
        for train, validation in folds:
            features, labels, groups = _matrix(train, policy["featureNames"])
            model = _fit(parameters, features, labels, groups, policy["randomSeed"])
            fold_metrics.append(_metrics(
                model, validation, policy["featureNames"], policy["businessPriorWeights"], policy["topK"]
            ))
        candidates.append({
            "parameters": parameters,
            "foldMetrics": fold_metrics,
            "meanAccuracy": round(float(np.mean([row["accuracy"] for row in fold_metrics])), 8),
            "meanPrecision": round(float(np.mean([row["precision"] for row in fold_metrics])), 8),
            "meanRecall": round(float(np.mean([row["recall"] for row in fold_metrics])), 8),
            "meanF1": round(float(np.mean([row["f1"] for row in fold_metrics])), 8),
            "meanRecallAtK": round(float(np.mean([row["recallAtK"] for row in fold_metrics])), 8),
        })
    selected = max(candidates, key=lambda row: (row["meanF1"], row["meanRecallAtK"], row["meanAccuracy"]))
    features, labels, groups = _matrix(sessions, policy["featureNames"])
    model = _fit(selected["parameters"], features, labels, groups, policy["randomSeed"])
    model_path.parent.mkdir(parents=True, exist_ok=True)
    model.save_model(model_path)
    in_sample = _metrics(
        model, sessions, policy["featureNames"], policy["businessPriorWeights"], policy["topK"]
    )
    scenario_metrics = _scenario_metrics(model, policy["featureNames"], policy["businessPriorWeights"])
    report = {
        "schemaVersion": 2,
        "reportVersion": "recommendation-diverse-development-v2",
        "datasetVersion": policy["datasetVersion"],
        "foldStrategy": "five-fold-rolling-origin",
        "folds": 5,
        "selectedParameters": selected["parameters"],
        "candidateReports": candidates,
        "trainingMetricsDefinition": "mean rolling-origin out-of-fold development metrics",
        "trainingMetrics": {
            "accuracy": selected["meanAccuracy"], "errorRate": round(1 - selected["meanAccuracy"], 8),
            "precision": selected["meanPrecision"], "recall": selected["meanRecall"],
            "f1": selected["meanF1"], "recallAtK": selected["meanRecallAtK"],
        },
        "inSampleDiagnostics": in_sample,
        "businessScenarioMetrics": scenario_metrics,
        "testOpened": False,
        "promotionAllowed": False,
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lock = {
        "schemaVersion": 1,
        "status": "model-selected-test-sealed",
        "developmentSessionsSha256": _sha(development_path),
        "sealedTestSha256": _sha(dataset_root / "test-sessions.sealed.jsonl"),
        "policySha256": _sha(policy_path),
        "developmentReportSha256": _sha(report_path),
        "modelSha256": _sha(model_path),
        "selectedParameters": selected["parameters"],
        "testOpenBudget": 1,
        "testOpenCount": 0,
    }
    lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def open_test(
    dataset_root: Path, policy_path: Path, development_report_path: Path,
    model_path: Path, lock_path: Path, result_path: Path, opening_record_path: Path,
) -> dict[str, Any]:
    """Abre una vez el test sellado, calcula métricas y conserva el resultado."""

    if opening_record_path.exists() or result_path.exists():
        raise ValueError("RECOMMENDATION_DIVERSE_TEST_ALREADY_OPENED")
    policy = _load_policy(policy_path)
    lock = json.loads(lock_path.read_text(encoding="utf-8"))
    expected = {
        "developmentSessionsSha256": _sha(dataset_root / "development-sessions.jsonl"),
        "sealedTestSha256": _sha(dataset_root / "test-sessions.sealed.jsonl"),
        "policySha256": _sha(policy_path),
        "developmentReportSha256": _sha(development_report_path),
        "modelSha256": _sha(model_path),
    }
    if lock.get("status") != "model-selected-test-sealed" or any(lock.get(key) != value for key, value in expected.items()):
        raise ValueError("RECOMMENDATION_DIVERSE_PRETEST_HASH_MISMATCH")
    from xgboost import XGBRanker

    model = XGBRanker()
    model.load_model(model_path)
    test_sessions = _read_jsonl(dataset_root / "test-sessions.sealed.jsonl")
    if {row["split"] for row in test_sessions} != {"test"}:
        raise ValueError("RECOMMENDATION_DIVERSE_TEST_SPLIT_INVALID")
    test_metrics = _metrics(
        model, test_sessions, policy["featureNames"], policy["businessPriorWeights"], policy["topK"]
    )
    development = json.loads(development_report_path.read_text(encoding="utf-8"))
    training = development["trainingMetrics"]
    gates = policy["gates"]
    gap = round(abs(training["accuracy"] - test_metrics["accuracy"]), 8)
    gates_passed = (
        training["accuracy"] <= gates["maximumTrainingAccuracy"]
        and test_metrics["accuracy"] >= gates["minimumTestAccuracy"]
        and test_metrics["errorRate"] < gates["maximumTestErrorExclusive"]
        and test_metrics["precision"] >= gates["minimumTestPrecision"]
        and test_metrics["recall"] >= gates["minimumTestRecall"]
        and test_metrics["f1"] >= gates["minimumTestF1"]
        and test_metrics["macroFamilyPrecision"] >= gates["minimumMacroFamilyMetric"]
        and test_metrics["macroFamilyRecall"] >= gates["minimumMacroFamilyMetric"]
        and test_metrics["macroFamilyF1"] >= gates["minimumMacroFamilyMetric"]
        and gap <= gates["maximumTrainTestGap"]
    )
    result = {
        "schemaVersion": 2,
        "reportVersion": "recommendation-cross-validation-diverse-v2",
        "modelVersion": policy["modelVersion"],
        "modelSha256": _sha(model_path),
        "datasetVersion": policy["datasetVersion"],
        "foldStrategy": "five-fold-rolling-origin",
        "folds": 5,
        "trainingMetrics": training,
        "inSampleDiagnostics": development["inSampleDiagnostics"],
        "testMetrics": test_metrics,
        "businessScenarioMetrics": development["businessScenarioMetrics"],
        "trainTestAccuracyGap": gap,
        "qualityGatesPassed": gates_passed,
        "productionEvidence": False,
        "promotionAllowed": False,
        "fallback": "deterministic-contextual-ranking",
        "limitations": [
            "El test es sintético y no demuestra conversión productiva ni causalidad.",
            "Desarrollo contiene weak labels más ambiguas que el test adjudicado; ambas tasas se publican.",
            "Los tipos son candidatos funcionales y la señal visual se limita a metadatos de ambiente.",
        ],
    }
    result_path.parent.mkdir(parents=True, exist_ok=True)
    result_path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    opening_record = {
        "schemaVersion": 1,
        "status": "consumed",
        "testOpenBudget": 1,
        "testOpenCount": 1,
        "pretestLockSha256": _sha(lock_path),
        "sealedTestSha256": expected["sealedTestSha256"],
        "resultSha256": _sha(result_path),
        "selectionUsedTest": False,
    }
    opening_record_path.write_text(
        json.dumps(opening_record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return result


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage", choices=("select", "open-test"))
    parser.add_argument("--dataset-root", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--development-report", type=Path, required=True)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--lock", type=Path, required=True)
    parser.add_argument("--result", type=Path)
    parser.add_argument("--opening-record", type=Path)
    args = parser.parse_args()
    if args.stage == "select":
        report = select_model(args.dataset_root, args.policy, args.development_report, args.model, args.lock)
        print(json.dumps({"training": report["trainingMetrics"], "testOpened": False}))
    else:
        if args.result is None or args.opening_record is None:
            parser.error("open-test requiere --result y --opening-record")
        report = open_test(
            args.dataset_root, args.policy, args.development_report, args.model,
            args.lock, args.result, args.opening_record,
        )
        print(json.dumps({"training": report["trainingMetrics"], "test": report["testMetrics"], "gates": report["qualityGatesPassed"]}))


if __name__ == "__main__":
    run()
