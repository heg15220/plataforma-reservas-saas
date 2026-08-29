"""Evaluación 5-fold del recomendador contextual sobre recorridos sintéticos.

Los folds son rolling-origin: cada validación ocurre después de su entrenamiento.
Las estadísticas de popularidad, reservas y horario se recalculan dentro de cada
fold para impedir leakage. El test temporal de junio se abre solo para el modelo
seleccionado con los cinco folds. Accuracy se acompaña siempre de precision,
recall y F1 top-1 para impedir que el desbalance de candidatos infle el resultado.
"""

from __future__ import annotations

import argparse
import json
import math
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import numpy as np


VISUAL_ATTRIBUTES = {"naturalLight", "privateCabin"}


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _time(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def _hour_bucket(value: str) -> int:
    hour = _time(value).hour
    return 0 if hour < 12 else 1 if hour < 18 else 2


@dataclass(frozen=True)
class HistorySnapshot:
    """Agregados pre-outcome calculados solo con sesiones históricas del fold."""

    venue_exposure: dict[str, float]
    profile_category: dict[tuple[str, str], float]
    profile_hour: dict[tuple[str, int], float]
    venue_hour: dict[tuple[str, int], float]


def _history(sessions: list[dict[str, Any]]) -> HistorySnapshot:
    exposures: Counter[str] = Counter()
    profile_categories: Counter[tuple[str, str]] = Counter()
    profile_totals: Counter[str] = Counter()
    profile_hours: Counter[tuple[str, int]] = Counter()
    venue_hours: Counter[tuple[str, int]] = Counter()
    venue_bookings: Counter[str] = Counter()
    for session in sessions:
        bucket = _hour_bucket(session["occurredAt"])
        for candidate in session["candidates"]:
            venue_id = candidate["venueId"]
            exposures[venue_id] += 1
            if candidate["labels"]["bookingCompleted"]:
                profile_categories[(session["profileId"], candidate["categoryCode"])] += 1
                profile_totals[session["profileId"]] += 1
                profile_hours[(session["profileId"], bucket)] += 1
                venue_hours[(venue_id, bucket)] += 1
                venue_bookings[venue_id] += 1
    maximum_exposure = max(exposures.values(), default=1)
    return HistorySnapshot(
        venue_exposure={key: value / maximum_exposure for key, value in exposures.items()},
        profile_category={
            key: value / max(1, profile_totals[key[0]])
            for key, value in profile_categories.items()
        },
        profile_hour={
            key: value / max(1, sum(profile_hours[(key[0], hour)] for hour in range(3)))
            for key, value in profile_hours.items()
        },
        venue_hour={
            key: value / max(1, venue_bookings[key[0]]) for key, value in venue_hours.items()
        },
    )


def _affinities(
    session: dict[str, Any],
    candidate: dict[str, Any],
    venue: dict[str, Any],
    profile: dict[str, Any],
    history: HistorySnapshot,
) -> dict[str, float]:
    preferences = profile["permittedPreferences"]
    service_weights = preferences["serviceWeights"]
    attribute_weights = preferences["attributeWeights"]
    intent = session["intentCode"]
    service_affinity = 1.0 if intent in venue["serviceCodes"] else 0.0
    attribute_affinity = (
        sum(attribute_weights.get(code, 0.0) for code in venue["attributeCodes"])
        / max(1, len(attribute_weights))
    )
    visual_codes = VISUAL_ATTRIBUTES.intersection(venue["attributeCodes"])
    visual_affinity = (
        sum(attribute_weights.get(code, 0.0) for code in visual_codes)
        / max(1, len(visual_codes))
    )
    base = candidate["features"]
    content = float(base["contentAffinity"])
    availability = float(base["availabilityRatio"])
    popularity = history.venue_exposure.get(candidate["venueId"], 0.0)
    bucket = _hour_bucket(session["occurredAt"])
    proximity = math.exp(-float(base["distanceMeters"]) / 6_000.0)
    return {
        "contentAffinity": content,
        "serviceAffinity": service_affinity,
        "attributeAffinity": min(1.0, attribute_affinity),
        "visualAmbienceAffinity": min(1.0, visual_affinity),
        "availabilityRatio": availability,
        "alignedScarcityOpportunity": content * (1.0 - availability),
        "qualityScore": float(base["qualityScore"]),
        "proximity": proximity,
        "priceFit": 1.0 - abs(float(base["priceTier"]) - 2.0) / 2.0,
        "lowExposureAffinity": content * (1.0 - popularity),
        "capacityOpportunity": content * (1.0 - availability) * (1.0 - popularity),
        "historicalCategoryAffinity": history.profile_category.get(
            (session["profileId"], candidate["categoryCode"]), 0.0
        ),
        "commonUserHourAffinity": history.profile_hour.get(
            (session["profileId"], bucket), 0.0
        ),
        "commonVenueHourAffinity": history.venue_hour.get((candidate["venueId"], bucket), 0.0),
        "isNewVenue": float(candidate["isNewVenue"]),
    }


def _matrix(
    sessions: list[dict[str, Any]],
    venues: dict[str, dict[str, Any]],
    profiles: dict[str, dict[str, Any]],
    history: HistorySnapshot,
    feature_names: list[str],
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    values: list[list[float]] = []
    labels: list[int] = []
    groups: list[int] = []
    for session in sorted(sessions, key=lambda row: (_time(row["occurredAt"]), row["sessionId"])):
        eligible = [
            candidate
            for candidate in session["candidates"]
            if candidate["eligible"] and candidate["capacityAvailable"]
        ]
        if len(eligible) != len(session["candidates"]):
            raise ValueError("RECOMMENDATION_CV_INELIGIBLE_CANDIDATE_PRESENT")
        groups.append(len(eligible))
        for candidate in eligible:
            feature_map = _affinities(
                session,
                candidate,
                venues[candidate["venueId"]],
                profiles[session["profileId"]],
                history,
            )
            values.append([feature_map[name] for name in feature_names])
            labels.append(candidate["labels"]["relevance"])
    return (
        np.asarray(values, dtype=np.float64),
        np.asarray(labels, dtype=np.int32),
        np.asarray(groups, dtype=np.uint32),
    )


def _rolling_folds(sessions: list[dict[str, Any]], folds: int) -> list[tuple[list[dict[str, Any]], list[dict[str, Any]]]]:
    ordered = sorted(sessions, key=lambda row: (_time(row["occurredAt"]), row["sessionId"]))
    boundaries = [round(len(ordered) * index / (folds + 1)) for index in range(folds + 2)]
    result = []
    for fold in range(folds):
        train = ordered[: boundaries[fold + 1]]
        validation = ordered[boundaries[fold + 1] : boundaries[fold + 2]]
        if not train or not validation or _time(train[-1]["occurredAt"]) > _time(validation[0]["occurredAt"]):
            raise ValueError("RECOMMENDATION_CV_TEMPORAL_FOLD_INVALID")
        result.append((train, validation))
    return result


def _fit(parameters: dict[str, Any], features: np.ndarray, labels: np.ndarray, groups: np.ndarray, seed: int):
    """Carga XGBoost de forma diferida para que contratos puros no requieran el extra ML."""

    from xgboost import XGBRanker

    model = XGBRanker(
        objective="rank:ndcg",
        eval_metric="ndcg@3",
        n_estimators=parameters["nEstimators"],
        max_depth=parameters["maximumDepth"],
        learning_rate=parameters["learningRate"],
        reg_lambda=parameters["l2Penalty"],
        subsample=0.85,
        colsample_bytree=0.9,
        random_state=seed,
        n_jobs=1,
        tree_method="hist",
        verbosity=0,
    )
    model.fit(features, labels, group=groups, verbose=False)
    return model


def _decision_metrics(
    model: Any,
    sessions: list[dict[str, Any]],
    venues: dict[str, dict[str, Any]],
    profiles: dict[str, dict[str, Any]],
    history: HistorySnapshot,
    feature_names: list[str],
    top_k: int,
    business_prior_weights: dict[str, float],
) -> dict[str, float | int]:
    true_positive = false_positive = false_negative = true_negative = 0
    top_k_hits = top_k_predictions = actual_positives = evaluated_sessions = 0
    for session in sessions:
        relevant = [
            index
            for index, candidate in enumerate(session["candidates"])
            if candidate["labels"]["relevance"] > 0
        ]
        if len(relevant) != 1:
            continue
        rows = []
        for candidate in session["candidates"]:
            feature_map = _affinities(
                session, candidate, venues[candidate["venueId"]], profiles[session["profileId"]], history
            )
            rows.append([feature_map[name] for name in feature_names])
        matrix = np.asarray(rows, dtype=np.float64)
        scores = model.predict(matrix)
        for name, weight in business_prior_weights.items():
            scores = scores + weight * matrix[:, feature_names.index(name)]
        order = sorted(range(len(scores)), key=lambda index: (-float(scores[index]), session["candidates"][index]["venueId"]))
        predicted = order[0]
        actual = relevant[0]
        evaluated_sessions += 1
        for index in range(len(rows)):
            expected_positive = index == actual
            predicted_positive = index == predicted
            true_positive += int(expected_positive and predicted_positive)
            false_positive += int(not expected_positive and predicted_positive)
            false_negative += int(expected_positive and not predicted_positive)
            true_negative += int(not expected_positive and not predicted_positive)
        selected = set(order[:top_k])
        top_k_hits += int(actual in selected)
        top_k_predictions += min(top_k, len(rows))
        actual_positives += 1
    total = true_positive + false_positive + false_negative + true_negative
    precision = true_positive / (true_positive + false_positive) if true_positive + false_positive else 0.0
    recall = true_positive / (true_positive + false_negative) if true_positive + false_negative else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    accuracy = (true_positive + true_negative) / total if total else 0.0
    return {
        "sessions": evaluated_sessions,
        "accuracy": round(accuracy, 8),
        "errorRate": round(1.0 - accuracy, 8),
        "precision": round(precision, 8),
        "recall": round(recall, 8),
        "f1": round(f1, 8),
        "top1HitRate": round(true_positive / evaluated_sessions, 8) if evaluated_sessions else 0.0,
        "precisionAtK": round(top_k_hits / top_k_predictions, 8) if top_k_predictions else 0.0,
        "recallAtK": round(top_k_hits / actual_positives, 8) if actual_positives else 0.0,
    }


def business_scenarios(feature_names: list[str]) -> list[dict[str, Any]]:
    """Casos contrafactuales congelados; no se derivan de predicciones del modelo."""

    def candidate(**updates: float) -> list[float]:
        values = {name: 0.35 for name in feature_names}
        values.update(updates)
        return [values[name] for name in feature_names]

    return [
        {
            "scenarioCode": "aligned-scarce-underexposed",
            "description": "Alta afinidad, poca exposición y pocas plazas frente a local popular poco alineado.",
            "expectedIndex": 0,
            "candidates": [
                candidate(contentAffinity=0.98, serviceAffinity=1.0, availabilityRatio=0.18,
                          alignedScarcityOpportunity=0.80, lowExposureAffinity=0.92,
                          capacityOpportunity=0.76, qualityScore=0.58),
                candidate(contentAffinity=0.30, serviceAffinity=0.0, availabilityRatio=0.9,
                          qualityScore=0.96, lowExposureAffinity=0.05),
            ],
        },
        {
            "scenarioCode": "visual-ambience-match",
            "description": "El ambiente visual permitido coincide con la preferencia, sin inferencias sensibles.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.8, visualAmbienceAffinity=1.0, attributeAffinity=0.9),
                           candidate(contentAffinity=0.8, visualAmbienceAffinity=0.0, attributeAffinity=0.2)],
        },
        {
            "scenarioCode": "common-user-time",
            "description": "Coincidencia con el rango horario habitual del usuario.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.8, commonUserHourAffinity=1.0, commonVenueHourAffinity=0.9),
                           candidate(contentAffinity=0.8, commonUserHourAffinity=0.0, commonVenueHourAffinity=0.1)],
        },
        {
            "scenarioCode": "nearby-compatible",
            "description": "Dos locales compatibles; prevalece el cercano dentro del rango.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.9, proximity=0.98), candidate(contentAffinity=0.9, proximity=0.08)],
        },
        {
            "scenarioCode": "specialty-match",
            "description": "La especialidad solicitada debe prevalecer sobre calidad genérica.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.9, serviceAffinity=1.0, qualityScore=0.7),
                           candidate(contentAffinity=0.4, serviceAffinity=0.0, qualityScore=0.98)],
        },
        {
            "scenarioCode": "repeat-booking-affinity",
            "description": "El historial de reservas de categoría refuerza una opción compatible.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.85, historicalCategoryAffinity=1.0),
                           candidate(contentAffinity=0.85, historicalCategoryAffinity=0.0)],
        },
        {
            "scenarioCode": "cold-start-exploration",
            "description": "Un local nuevo y poco expuesto puede competir si está muy alineado.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.95, serviceAffinity=1.0, isNewVenue=1.0,
                                          lowExposureAffinity=0.95, qualityScore=0.65),
                           candidate(contentAffinity=0.45, serviceAffinity=0.0, isNewVenue=0.0,
                                     qualityScore=0.9)],
        },
        {
            "scenarioCode": "quality-does-not-override-intent",
            "description": "Una valoración alta no debe compensar incompatibilidad de intención.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.95, serviceAffinity=1.0, qualityScore=0.55),
                           candidate(contentAffinity=0.2, serviceAffinity=0.0, qualityScore=1.0)],
        },
        {
            "scenarioCode": "available-capacity",
            "description": "Entre opciones alineadas, se recomienda la que conserva capacidad reservable.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.9, availabilityRatio=0.75),
                           candidate(contentAffinity=0.9, availabilityRatio=0.05)],
        },
        {
            "scenarioCode": "price-and-distance-balance",
            "description": "Precio compatible y cercanía vencen una alternativa lejana y cara.",
            "expectedIndex": 0,
            "candidates": [candidate(contentAffinity=0.82, priceFit=1.0, proximity=0.9),
                           candidate(contentAffinity=0.82, priceFit=0.0, proximity=0.1)],
        },
    ]


def _evaluate_business_scenarios(
    model: Any, feature_names: list[str], business_prior_weights: dict[str, float]
) -> dict[str, Any]:
    cases = business_scenarios(feature_names)
    results = []
    for case in cases:
        matrix = np.asarray(case["candidates"], dtype=np.float64)
        scores = model.predict(matrix)
        for name, weight in business_prior_weights.items():
            scores = scores + weight * matrix[:, feature_names.index(name)]
        predicted = int(np.argmax(scores))
        results.append(
            {
                "scenarioCode": case["scenarioCode"],
                "passed": predicted == case["expectedIndex"],
                "expectedIndex": case["expectedIndex"],
                "predictedIndex": predicted,
            }
        )
    passed = sum(result["passed"] for result in results)
    accuracy = passed / len(results)
    return {
        "cases": len(results),
        "passed": passed,
        "accuracy": round(accuracy, 8),
        "errorRate": round(1.0 - accuracy, 8),
        "precision": round(accuracy, 8),
        "recall": round(accuracy, 8),
        "f1": round(accuracy, 8),
        "results": results,
    }


def evaluate_recommendation_cv(
    dataset_root: Path,
    policy_path: Path,
    output_path: Path,
    model_output_path: Path,
) -> dict[str, Any]:
    policy = json.loads(policy_path.read_text(encoding="utf-8"))
    if policy.get("folds") != 5 or policy.get("automaticPromotionAllowed") is not False:
        raise ValueError("RECOMMENDATION_CV_POLICY_INVALID")
    venues = {row["venueId"]: row for row in _read_jsonl(dataset_root / "venues.jsonl")}
    profiles = {row["profileId"]: row for row in _read_jsonl(dataset_root / "profiles.jsonl")}
    sessions = _read_jsonl(dataset_root / "ranking-sessions.jsonl")
    development = [row for row in sessions if row["split"] in {"train", "validation"}]
    test = [row for row in sessions if row["split"] == "test"]
    folds = _rolling_folds(development, policy["folds"])
    candidate_reports = []
    for parameters in policy["hyperparameters"]:
        fold_metrics = []
        for train, validation in folds:
            history = _history(train)
            features, labels, groups = _matrix(
                train, venues, profiles, history, policy["featureNames"]
            )
            model = _fit(parameters, features, labels, groups, policy["randomSeed"])
            fold_metrics.append(
                _decision_metrics(
                    model, validation, venues, profiles, history, policy["featureNames"],
                    policy["topK"], policy["businessPriorWeights"]
                )
            )
        candidate_reports.append(
            {
                "parameters": parameters,
                "foldMetrics": fold_metrics,
                "meanAccuracy": round(float(np.mean([row["accuracy"] for row in fold_metrics])), 8),
                "meanF1": round(float(np.mean([row["f1"] for row in fold_metrics])), 8),
                "meanRecallAtK": round(float(np.mean([row["recallAtK"] for row in fold_metrics])), 8),
            }
        )
    selected = max(
        candidate_reports,
        key=lambda row: (row["meanF1"], row["meanRecallAtK"], row["meanAccuracy"]),
    )
    history = _history(development)
    features, labels, groups = _matrix(
        development, venues, profiles, history, policy["featureNames"]
    )
    model = _fit(selected["parameters"], features, labels, groups, policy["randomSeed"])
    model.save_model(model_output_path)
    import hashlib

    model_sha256 = hashlib.sha256(model_output_path.read_bytes()).hexdigest()
    training_metrics = _decision_metrics(
        model, development, venues, profiles, history, policy["featureNames"],
        policy["topK"], policy["businessPriorWeights"]
    )
    test_metrics = _decision_metrics(
        model, test, venues, profiles, history, policy["featureNames"],
        policy["topK"], policy["businessPriorWeights"]
    )
    scenario_metrics = _evaluate_business_scenarios(
        model, policy["featureNames"], policy["businessPriorWeights"]
    )
    gap = round(abs(training_metrics["accuracy"] - test_metrics["accuracy"]), 8)
    gates = policy["gates"]
    quality_passed = (
        training_metrics["accuracy"] <= gates["maximumTrainingDecisionAccuracy"]
        and test_metrics["accuracy"] >= gates["minimumTestDecisionAccuracy"]
        and test_metrics["errorRate"] <= gates["maximumTestDecisionError"]
        and test_metrics["precision"] >= gates["minimumTestPrecision"]
        and test_metrics["recall"] >= gates["minimumTestRecall"]
        and test_metrics["f1"] >= gates["minimumTestF1"]
        and gap <= gates["maximumTrainTestAccuracyGap"]
    )
    report = {
        "schemaVersion": 1,
        "reportVersion": "recommendation-cross-validation-v1",
        "modelVersion": policy["modelVersion"],
        "modelPath": str(model_output_path).replace("\\", "/"),
        "modelSha256": model_sha256,
        "datasetVersion": policy["datasetVersion"],
        "foldStrategy": "five-fold-rolling-origin",
        "folds": 5,
        "selectedParameters": selected["parameters"],
        "candidateReports": candidate_reports,
        "trainingMetrics": training_metrics,
        "crossValidationMetrics": {
            "accuracy": selected["meanAccuracy"],
            "f1": selected["meanF1"],
            "recallAtK": selected["meanRecallAtK"],
        },
        "testMetrics": test_metrics,
        "businessScenarioMetrics": scenario_metrics,
        "trainTestAccuracyGap": gap,
        "qualityGatesPassed": quality_passed,
        "productionEvidence": False,
        "promotionAllowed": False,
        "limitations": [
            "Observed choices include deliberate stochastic noise and are not deterministic compatibility labels.",
            "Candidate-level accuracy must be interpreted with precision, recall and F1 because each query has seven negatives.",
            "Synthetic results do not demonstrate production conversion or causality."
        ],
    }
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def run() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset-root", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--model-output", type=Path, required=True)
    args = parser.parse_args()
    report = evaluate_recommendation_cv(
        args.dataset_root, args.policy, args.output, args.model_output
    )
    print(json.dumps({"training": report["trainingMetrics"], "test": report["testMetrics"], "gates": report["qualityGatesPassed"]}))


if __name__ == "__main__":
    run()
