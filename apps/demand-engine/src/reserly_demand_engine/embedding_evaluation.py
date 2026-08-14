"""Evaluación offline reproducible de calidad y latencia del encoder aprobado."""

from __future__ import annotations

import json
import math
import statistics
import time
from dataclasses import asdict, dataclass
from datetime import UTC, datetime
from pathlib import Path

from .embeddings import EmbeddingModelManifest, SentenceTransformerEmbedder, TextEmbedder


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    """Métricas suficientes para una puerta de promoción del baseline semántico."""

    datasetVersion: str
    modelVersion: str
    evaluatedAt: str
    queryCount: int
    documentCount: int
    recallAt1: float
    recallAt3: float
    meanReciprocalRank: float
    crossLocaleRecallAt3: float
    warmQueryP95Ms: float
    warmDocumentPerItemP95Ms: float
    qualityPassed: bool
    latencyPassed: bool


def evaluate(
    manifest: EmbeddingModelManifest,
    dataset: dict[str, object],
    embedder: TextEmbedder,
    latency_repetitions: int = 5,
) -> EvaluationResult:
    """Calcula recall/MRR por coseno y p95 warm sin guardar embeddings en el informe."""
    documents = dataset["documents"]
    queries = dataset["queries"]
    if not isinstance(documents, list) or not isinstance(queries, list) or not queries:
        raise ValueError("EVALUATION_DATASET_INVALID")
    document_vectors = embedder.encode_documents([item["text"] for item in documents])
    query_vectors = embedder.encode_queries([item["text"] for item in queries])
    document_ids = [item["id"] for item in documents]
    recall_one = 0
    recall_three = 0
    reciprocal_ranks: list[float] = []
    cross_total = 0
    cross_hits = 0
    locale_by_document = {item["id"]: item["locale"] for item in documents}
    for query, query_vector in zip(queries, query_vectors, strict=True):
        ranked = sorted(
            zip(document_ids, document_vectors, strict=True),
            key=lambda item: _dot(query_vector, item[1]),
            reverse=True,
        )
        ranked_ids = [item[0] for item in ranked]
        relevant = set(query["relevantDocumentIds"])
        recall_one += int(bool(relevant & set(ranked_ids[:1])))
        recall_three += int(bool(relevant & set(ranked_ids[:3])))
        rank = next(index for index, value in enumerate(ranked_ids, 1) if value in relevant)
        reciprocal_ranks.append(1 / rank)
        if all(locale_by_document[value] != query["locale"] for value in relevant):
            cross_total += 1
            cross_hits += int(bool(relevant & set(ranked_ids[:3])))

    query_latencies, document_latencies = _measure_latency(
        embedder, queries[0]["text"], documents, latency_repetitions
    )
    count = len(queries)
    recall_at_one = recall_one / count
    recall_at_three = recall_three / count
    mrr = statistics.fmean(reciprocal_ranks)
    cross_recall = cross_hits / cross_total if cross_total else 0.0
    query_p95 = _percentile_95(query_latencies)
    document_p95 = _percentile_95(document_latencies)
    quality = manifest.qualityThresholds
    latency = manifest.latencyThresholdsMs
    return EvaluationResult(
        datasetVersion=str(dataset["datasetVersion"]),
        modelVersion=manifest.modelKey,
        evaluatedAt=datetime.now(UTC).isoformat(),
        queryCount=count,
        documentCount=len(documents),
        recallAt1=round(recall_at_one, 6),
        recallAt3=round(recall_at_three, 6),
        meanReciprocalRank=round(mrr, 6),
        crossLocaleRecallAt3=round(cross_recall, 6),
        warmQueryP95Ms=round(query_p95, 3),
        warmDocumentPerItemP95Ms=round(document_p95, 3),
        qualityPassed=(
            recall_at_one >= quality.recallAt1
            and recall_at_three >= quality.recallAt3
            and mrr >= quality.meanReciprocalRank
            and cross_recall >= quality.crossLocaleRecallAt3
        ),
        latencyPassed=(
            query_p95 <= latency.warmQueryP95
            and document_p95 <= latency.warmDocumentPerItemP95
        ),
    )


def run() -> None:
    """CLI explícita: descarga/carga el modelo pinneado y escribe solo métricas agregadas."""
    root = Path(__file__).resolve().parents[2]
    manifest = EmbeddingModelManifest.load(root / "models/multilingual-e5-small.v1.json")
    dataset = json.loads(
        (root / "evaluation/personal-care-retrieval.v1.json").read_text(encoding="utf-8")
    )
    result = evaluate(manifest, dataset, SentenceTransformerEmbedder(manifest))
    print(json.dumps(asdict(result), indent=2, sort_keys=True))
    if not (result.qualityPassed and result.latencyPassed):
        raise SystemExit(1)


def _measure_latency(
    embedder: TextEmbedder,
    query: str,
    documents: list[dict[str, object]],
    repetitions: int,
) -> tuple[list[float], list[float]]:
    embedder.encode_queries([query])
    batch = [item["text"] for item in documents]
    embedder.encode_documents(batch)
    query_ms: list[float] = []
    documents_ms: list[float] = []
    for _ in range(max(repetitions, 1)):
        started = time.perf_counter()
        embedder.encode_queries([query])
        query_ms.append((time.perf_counter() - started) * 1_000)
        started = time.perf_counter()
        embedder.encode_documents(batch)
        documents_ms.append((time.perf_counter() - started) * 1_000 / len(batch))
    return query_ms, documents_ms


def _dot(left: list[float], right: list[float]) -> float:
    return sum(a * b for a, b in zip(left, right, strict=True))


def _percentile_95(values: list[float]) -> float:
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, math.ceil(len(ordered) * 0.95) - 1)]


if __name__ == "__main__":
    run()
