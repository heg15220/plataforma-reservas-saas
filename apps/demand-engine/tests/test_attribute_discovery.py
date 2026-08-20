"""Pruebas reales de UMAP/HDBSCAN/BERTopic/c-TF-IDF y revisión humana obligatoria."""

from __future__ import annotations

import hashlib
import unittest
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.attribute_discovery import (
    AttributeDiscoveryDataset,
    AttributeDiscoveryPipeline,
    AttributeDiscoveryPolicy,
)

ROOT = Path(__file__).parents[1]


class _ClusteredEmbedder:
    """Embedding semántico sintético de 384 dimensiones para ejercitar el stack real."""

    dimensions = 384

    def encode_queries(self, texts):
        return self.encode_documents(texts)

    def encode_documents(self, texts):
        vectors = []
        for text in texts:
            lowered = text.casefold()
            cluster = 0 if "espera" in lowered or "waiting" in lowered else 1 if "silenc" in lowered or "quiet" in lowered else 2
            vector = [0.0] * self.dimensions
            vector[cluster] = 1.0
            digest = hashlib.sha256(text.encode("utf-8")).digest()
            for index in range(3, 15):
                vector[index] = digest[index] / 25500
            vectors.append(vector)
        return vectors


class AttributeDiscoveryTests(unittest.TestCase):
    """Valida clusters bilingües estables, minimización y fallos cerrados."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = AttributeDiscoveryPolicy.load(ROOT / "policies" / "attribute-discovery.v1.json")
        cls.pipeline = AttributeDiscoveryPipeline(
            cls.policy,
            ROOT.parents[1] / "packages" / "demand-contracts" / "ontology" / "personal-care.v1.json",
        )
        cls.dataset = cls._dataset()
        cls.result = cls.pipeline.discover(cls.dataset, _ClusteredEmbedder())

    @staticmethod
    def _dataset():
        groups = [
            ("espera breve puntual cita rápida número", "short waiting punctual quick appointment number"),
            ("ambiente silencioso tranquilo relajado número", "quiet calm relaxing atmosphere number"),
            ("asesoramiento atento escucha cuidado número", "attentive advice listening care number"),
        ]
        documents = []
        for spanish, english in groups:
            for index in range(6):
                documents.append({"documentId": str(uuid4()), "locale": "es", "source": "verifiedReview", "text": spanish.replace("número", str(index))})
                documents.append({"documentId": str(uuid4()), "locale": "en", "source": "verifiedReview", "text": english.replace("number", str(index))})
        return AttributeDiscoveryDataset.model_validate({
            "datasetVersion": "attribute-discovery-synthetic-v1",
            "ontologyVersion": "personal-care.v1",
            "embeddingModelVersion": "multilingual-e5-small-v1",
            "productionEvidence": False,
            "purpose": "ontologyDiscovery",
            "containsPersonalData": False,
            "piiScanPassed": True,
            "sensitiveTermsRemoved": True,
            "consentRevocationsApplied": True,
            "documents": documents,
        })

    def test_executes_real_stack_and_only_emits_review_candidates(self) -> None:
        result = self.result
        self.assertEqual(["embeddings", "UMAP", "HDBSCAN", "BERTopic", "c-TF-IDF"], result.componentsExecuted)
        self.assertGreaterEqual(len(result.candidates), 2, result)
        self.assertFalse(result.automaticPublicationAllowed)
        for candidate in result.candidates:
            self.assertEqual("pendingHumanReview", candidate.status)
            self.assertFalse(candidate.automaticPublicationAllowed)
            self.assertEqual("ROLE_ADMIN", candidate.requiredReviewRole)
            self.assertGreaterEqual(candidate.documentsByLocale["es"], 2)
            self.assertGreaterEqual(candidate.documentsByLocale["en"], 2)
            self.assertGreater(candidate.meanMembershipProbability, 0)

    def test_output_contains_terms_and_references_but_not_source_text(self) -> None:
        dataset = self.dataset
        payload = self.result.model_dump_json()
        self.assertNotIn(dataset.documents[0].text, payload)
        self.assertNotIn("text\"", payload)
        self.assertIn("ctfidfScores", payload)

    def test_rejects_pii_sensitive_terms_and_version_drift(self) -> None:
        raw = self._dataset().model_dump()
        raw["documents"][0]["text"] = "Contacta test@example.com para ambiente tranquilo"
        with self.assertRaisesRegex(ValueError, "PRIVACY_REJECTED"):
            self.pipeline.discover(AttributeDiscoveryDataset.model_validate(raw), _ClusteredEmbedder())
        raw = self._dataset().model_dump()
        raw["documents"][0]["text"] = "Diagnóstico de salud para el servicio"
        with self.assertRaisesRegex(ValueError, "SENSITIVE_TERM_REJECTED"):
            self.pipeline.discover(AttributeDiscoveryDataset.model_validate(raw), _ClusteredEmbedder())
        raw = self._dataset().model_dump()
        raw["embeddingModelVersion"] = "unknown-model-v1"
        with self.assertRaisesRegex(ValueError, "VERSION_OR_SIZE_MISMATCH"):
            self.pipeline.discover(AttributeDiscoveryDataset.model_validate(raw), _ClusteredEmbedder())


if __name__ == "__main__":
    unittest.main()
