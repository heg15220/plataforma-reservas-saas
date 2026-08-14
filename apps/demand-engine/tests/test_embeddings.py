"""Pruebas unitarias del manifiesto y la evaluación sin descargar modelos externos."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

from pydantic import ValidationError

from reserly_demand_engine.embedding_evaluation import evaluate
from reserly_demand_engine.embeddings import (
    EmbeddingModelManifest,
    canonical_content_checksum,
)


ROOT = Path(__file__).resolve().parents[1]


class _ExactFakeEmbedder:
    """Encoder ortogonal determinista para probar el cálculo, no la calidad del modelo real."""

    dimensions = 2

    def encode_queries(self, texts: list[str]) -> list[list[float]]:
        return [[1.0, 0.0] if "corte" in text else [0.0, 1.0] for text in texts]

    def encode_documents(self, texts: list[str]) -> list[list[float]]:
        return [[1.0, 0.0] if "corte" in text else [0.0, 1.0] for text in texts]


class EmbeddingContractTests(unittest.TestCase):
    def test_manifest_pins_license_revision_dimensions_locales_and_prompts(self) -> None:
        manifest = EmbeddingModelManifest.load(
            ROOT / "models/multilingual-e5-small.v1.json"
        )
        self.assertEqual("MIT", manifest.license)
        self.assertEqual(40, len(manifest.revision))
        self.assertEqual(384, manifest.dimensions)
        self.assertEqual({"es", "en"}, set(manifest.requiredLocales))
        self.assertEqual("query: ", manifest.queryPrefix)
        self.assertEqual("passage: ", manifest.documentPrefix)
        self.assertFalse(manifest.trustRemoteCode)

    def test_manifest_rejects_mutable_revision_and_unsupported_locale(self) -> None:
        raw = json.loads((ROOT / "models/multilingual-e5-small.v1.json").read_text("utf-8"))
        raw["revision"] = "main"
        raw["requiredLocales"] = ["es", "fr"]
        with self.assertRaises(ValidationError):
            EmbeddingModelManifest.model_validate(raw)

    def test_checksum_is_locale_aware_stable_and_contains_no_text(self) -> None:
        first = canonical_content_checksum("es", "  corte   de pelo ")
        self.assertEqual(first, canonical_content_checksum("es", "corte de pelo"))
        self.assertNotEqual(first, canonical_content_checksum("en", "corte de pelo"))
        self.assertEqual(64, len(first))
        self.assertNotIn("corte", first)

    def test_evaluator_calculates_recall_mrr_cross_locale_and_latency(self) -> None:
        manifest = EmbeddingModelManifest.load(
            ROOT / "models/multilingual-e5-small.v1.json"
        )
        dataset = {
            "datasetVersion": "unit-v1",
            "documents": [
                {"id": "cut", "locale": "en", "text": "corte"},
                {"id": "nails", "locale": "es", "text": "uñas"},
            ],
            "queries": [
                {"id": "q1", "locale": "es", "text": "corte", "relevantDocumentIds": ["cut"]},
                {"id": "q2", "locale": "en", "text": "uñas", "relevantDocumentIds": ["nails"]},
            ],
        }
        result = evaluate(manifest, dataset, _ExactFakeEmbedder(), latency_repetitions=1)
        self.assertEqual(1.0, result.recallAt1)
        self.assertEqual(1.0, result.recallAt3)
        self.assertEqual(1.0, result.meanReciprocalRank)
        self.assertEqual(1.0, result.crossLocaleRecallAt3)
        self.assertTrue(result.qualityPassed)


if __name__ == "__main__":
    unittest.main()
