"""Evaluación offline reproducible de calidad y latencia del encoder aprobado."""

from __future__ import annotations

import json
import math
import statistics
import time
from dataclasses import asdict, dataclass
from datetime import UTC, datetime
from pathlib import Path

from .embeddings import (
    EmbeddingModelManifest,
    SentenceTransformerEmbedder,
    TextEmbedder,
    compose_localized_document_text,
)


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    """Métricas suficientes para una puerta de promoción del baseline semántico."""

    datasetVersion: str
    modelVersion: str
    evaluatedAt: str
    queryCount: int
    developmentQueryCount: int
    holdoutQueryCount: int
    documentCount: int
    recallAt1: float
    recallAt3: float
    meanReciprocalRank: float
    crossLocaleRecallAt3: float
    developmentRecallAt1: float
    developmentRecallAt3: float
    developmentMeanReciprocalRank: float
    developmentCrossLocaleRecallAt3: float
    recallAt3GeneralizationGap: float
    mrrGeneralizationGap: float
    warmQueryP95Ms: float
    warmDocumentPerItemP95Ms: float
    qualityPassed: bool
    generalizationPassed: bool
    latencyPassed: bool


@dataclass(frozen=True, slots=True)
class _RetrievalMetrics:
    """Métricas internas de un split calculadas sobre el mismo corpus candidato."""

    recallAt1: float
    recallAt3: float
    meanReciprocalRank: float
    crossLocaleRecallAt3: float


def evaluate(
    manifest: EmbeddingModelManifest,
    dataset: dict[str, object],
    embedder: TextEmbedder,
    latency_repetitions: int = 5,
) -> EvaluationResult:
    """Calcula recall/MRR por coseno y p95 warm sin guardar embeddings en el informe."""
    documents = dataset["documents"]
    queries = dataset["queries"]
    if (
        not isinstance(documents, list)
        or not documents
        or not isinstance(queries, list)
        or not queries
    ):
        raise ValueError("EVALUATION_DATASET_INVALID")
    _validate_dataset(documents, queries)
    document_texts = [
        compose_localized_document_text(
            item["locale"], item["text"], item.get("localizedTexts")
        )
        for item in documents
    ]
    document_vectors = embedder.encode_documents(document_texts)
    query_vectors = embedder.encode_queries([item["text"] for item in queries])
    document_ids = [item["id"] for item in documents]
    locale_by_document = {item["id"]: item["locale"] for item in documents}
    explicit_splits = any("split" in query for query in queries)
    if explicit_splits and any(
        query.get("split") not in {"development", "holdout"} for query in queries
    ):
        raise ValueError("EVALUATION_SPLIT_INVALID")
    development_indexes = [
        index for index, query in enumerate(queries) if query.get("split") == "development"
    ]
    holdout_indexes = [
        index for index, query in enumerate(queries) if query.get("split") == "holdout"
    ]
    if explicit_splits and (not development_indexes or not holdout_indexes):
        raise ValueError("EVALUATION_SPLIT_INVALID")
    if not explicit_splits:
        development_indexes = holdout_indexes = list(range(len(queries)))

    development = _calculate_metrics(
        document_ids,
        document_vectors,
        queries,
        query_vectors,
        development_indexes,
        locale_by_document,
    )
    holdout = _calculate_metrics(
        document_ids,
        document_vectors,
        queries,
        query_vectors,
        holdout_indexes,
        locale_by_document,
    )

    query_latencies, document_latencies = _measure_latency(
        embedder, queries[0]["text"], document_texts, latency_repetitions
    )
    recall_gap = max(0.0, development.recallAt3 - holdout.recallAt3)
    mrr_gap = max(0.0, development.meanReciprocalRank - holdout.meanReciprocalRank)
    query_p95 = _percentile_95(query_latencies)
    document_p95 = _percentile_95(document_latencies)
    quality = manifest.qualityThresholds
    latency = manifest.latencyThresholdsMs
    generalization_passed = (
        recall_gap <= quality.maximumRecallAt3GeneralizationGap
        and mrr_gap <= quality.maximumMrrGeneralizationGap
    )
    return EvaluationResult(
        datasetVersion=str(dataset["datasetVersion"]),
        modelVersion=manifest.modelKey,
        evaluatedAt=datetime.now(UTC).isoformat(),
        queryCount=len(queries),
        developmentQueryCount=len(development_indexes),
        holdoutQueryCount=len(holdout_indexes),
        documentCount=len(documents),
        recallAt1=round(holdout.recallAt1, 6),
        recallAt3=round(holdout.recallAt3, 6),
        meanReciprocalRank=round(holdout.meanReciprocalRank, 6),
        crossLocaleRecallAt3=round(holdout.crossLocaleRecallAt3, 6),
        developmentRecallAt1=round(development.recallAt1, 6),
        developmentRecallAt3=round(development.recallAt3, 6),
        developmentMeanReciprocalRank=round(development.meanReciprocalRank, 6),
        developmentCrossLocaleRecallAt3=round(development.crossLocaleRecallAt3, 6),
        recallAt3GeneralizationGap=round(recall_gap, 6),
        mrrGeneralizationGap=round(mrr_gap, 6),
        warmQueryP95Ms=round(query_p95, 3),
        warmDocumentPerItemP95Ms=round(document_p95, 3),
        qualityPassed=(
            holdout.recallAt1 >= quality.recallAt1
            and holdout.recallAt3 >= quality.recallAt3
            and holdout.meanReciprocalRank >= quality.meanReciprocalRank
            and holdout.crossLocaleRecallAt3 >= quality.crossLocaleRecallAt3
            and generalization_passed
        ),
        generalizationPassed=generalization_passed,
        latencyPassed=(
            query_p95 <= latency.warmQueryP95
            and document_p95 <= latency.warmDocumentPerItemP95
        ),
    )


def _validate_dataset(
    documents: list[dict[str, object]], queries: list[dict[str, object]]
) -> None:
    """Rechaza IDs ambiguos, relevantes inexistentes y fuga textual exacta entre splits."""
    document_ids = [item.get("id") for item in documents]
    query_ids = [item.get("id") for item in queries]
    if len(document_ids) != len(set(document_ids)) or len(query_ids) != len(set(query_ids)):
        raise ValueError("EVALUATION_DATASET_DUPLICATE_ID")
    known_documents = set(document_ids)
    normalized_by_split: dict[str, set[str]] = {"development": set(), "holdout": set()}
    for query in queries:
        relevant = query.get("relevantDocumentIds")
        if not isinstance(relevant, list) or not relevant or not set(relevant) <= known_documents:
            raise ValueError("EVALUATION_RELEVANCE_INVALID")
        split = query.get("split")
        if split in normalized_by_split:
            normalized = " ".join(str(query.get("text", "")).casefold().split())
            normalized_by_split[split].add(normalized)
    if normalized_by_split["development"] & normalized_by_split["holdout"]:
        raise ValueError("EVALUATION_SPLIT_LEAKAGE")


def _calculate_metrics(
    document_ids: list[object],
    document_vectors: list[list[float]],
    queries: list[dict[str, object]],
    query_vectors: list[list[float]],
    indexes: list[int],
    locale_by_document: dict[object, object],
) -> _RetrievalMetrics:
    """Calcula relevancia de un split sin ajustar parámetros ni alterar candidatos."""
    recall_one = 0
    recall_three = 0
    reciprocal_ranks: list[float] = []
    cross_total = 0
    cross_hits = 0
    for index in indexes:
        query = queries[index]
        ranked = sorted(
            zip(document_ids, document_vectors, strict=True),
            key=lambda item: _dot(query_vectors[index], item[1]),
            reverse=True,
        )
        ranked_ids = [item[0] for item in ranked]
        relevant = set(query["relevantDocumentIds"])
        recall_one += int(bool(relevant & set(ranked_ids[:1])))
        recall_three += int(bool(relevant & set(ranked_ids[:3])))
        rank = next(position for position, value in enumerate(ranked_ids, 1) if value in relevant)
        reciprocal_ranks.append(1 / rank)
        if all(locale_by_document[value] != query["locale"] for value in relevant):
            cross_total += 1
            cross_hits += int(bool(relevant & set(ranked_ids[:3])))
    count = len(indexes)
    if cross_total == 0:
        raise ValueError("EVALUATION_CROSS_LOCALE_COVERAGE_MISSING")
    return _RetrievalMetrics(
        recallAt1=recall_one / count,
        recallAt3=recall_three / count,
        meanReciprocalRank=statistics.fmean(reciprocal_ranks),
        crossLocaleRecallAt3=cross_hits / cross_total,
    )


def run() -> None:
    """CLI explícita: descarga/carga el modelo pinneado y escribe solo métricas agregadas."""
    root = Path(__file__).resolve().parents[2]
    manifest = EmbeddingModelManifest.load(root / "models/multilingual-e5-small.v2.json")
    dataset = json.loads(
        (root / "evaluation/personal-care-retrieval.v2.json").read_text(encoding="utf-8")
    )
    result = evaluate(manifest, dataset, SentenceTransformerEmbedder(manifest))
    print(json.dumps(asdict(result), indent=2, sort_keys=True))
    if not (result.qualityPassed and result.latencyPassed):
        raise SystemExit(1)


def _measure_latency(
    embedder: TextEmbedder,
    query: str,
    document_texts: list[str],
    repetitions: int,
) -> tuple[list[float], list[float]]:
    embedder.encode_queries([query])
    embedder.encode_documents(document_texts)
    query_ms: list[float] = []
    documents_ms: list[float] = []
    for _ in range(max(repetitions, 1)):
        started = time.perf_counter()
        embedder.encode_queries([query])
        query_ms.append((time.perf_counter() - started) * 1_000)
        started = time.perf_counter()
        embedder.encode_documents(document_texts)
        documents_ms.append((time.perf_counter() - started) * 1_000 / len(document_texts))
    return query_ms, documents_ms


def _dot(left: list[float], right: list[float]) -> float:
    return sum(a * b for a, b in zip(left, right, strict=True))


def _percentile_95(values: list[float]) -> float:
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, math.ceil(len(ordered) * 0.95) - 1)]


if __name__ == "__main__":
    run()
