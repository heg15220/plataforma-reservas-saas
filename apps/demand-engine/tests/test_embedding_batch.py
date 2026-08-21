"""Pruebas del lote vectorial sin descargar el modelo real."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.embedding_batch import (
    EmbeddingBatchProcessor,
    EmbeddingBatchRequest,
)
from reserly_demand_engine.embeddings import canonical_content_checksum


class _FakeEmbedder:
    dimensions = 384

    def __init__(self) -> None:
        self.queries: list[str] = []
        self.documents: list[str] = []

    def encode_queries(self, texts: list[str]) -> list[list[float]]:
        self.queries.extend(texts)
        return [[1.0] + [0.0] * 383 for _ in texts]

    def encode_documents(self, texts: list[str]) -> list[list[float]]:
        self.documents.extend(texts)
        return [[0.0, 1.0] + [0.0] * 382 for _ in texts]


class EmbeddingBatchTests(unittest.TestCase):
    def test_separates_query_and_document_roles_and_preserves_order(self) -> None:
        now = datetime.now(UTC)
        query_id, venue_id, service_id = uuid4(), uuid4(), uuid4()
        request = EmbeddingBatchRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "subjects": [
                    {"subjectId": str(query_id), "subjectType": "query", "locale": "es", "text": "corte", "validFrom": now, "expiresAt": now + timedelta(hours=1)},
                    {"subjectId": str(venue_id), "subjectType": "venue", "locale": "en", "text": "salon", "validFrom": now},
                    {"subjectId": str(service_id), "subjectType": "service", "locale": "es", "text": "mechas", "validFrom": now},
                ],
            }
        )
        embedder = _FakeEmbedder()
        result = EmbeddingBatchProcessor(embedder, "multilingual-e5-small-v1").generate(request)
        self.assertEqual([query_id, venue_id, service_id], [item.subjectId for item in result.embeddings])
        self.assertEqual(["corte"], embedder.queries)
        self.assertEqual(["salon", "mechas"], embedder.documents)
        self.assertTrue(all(len(item.embedding) == 384 for item in result.embeddings))
        self.assertTrue(all(len(item.contentChecksum) == 64 for item in result.embeddings))

    def test_query_requires_expiry_and_contract_rejects_unknown_fields(self) -> None:
        with self.assertRaises(ValidationError):
            EmbeddingBatchRequest.model_validate(
                {"requestId": str(uuid4()), "subjects": [{"subjectId": str(uuid4()), "subjectType": "query", "locale": "es", "text": "corte", "validFrom": datetime.now(UTC), "rawEmail": "forbidden"}]}
            )

    def test_document_composes_governed_localizations_and_checksum_tracks_them(self) -> None:
        now = datetime.now(UTC)
        subject_id = uuid4()
        request = EmbeddingBatchRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "subjects": [
                    {
                        "subjectId": str(subject_id),
                        "subjectType": "service",
                        "locale": "es",
                        "text": "Corte de cabello",
                        "localizedTexts": {
                            "es": "Corte de cabello y recorte de puntas",
                            "en": "Haircut and split-end trim",
                        },
                        "validFrom": now,
                    }
                ],
            }
        )
        embedder = _FakeEmbedder()
        result = EmbeddingBatchProcessor(
            embedder, "multilingual-e5-small-v2", "localized_fields"
        ).generate(request)
        self.assertEqual(
            ["Corte de cabello\nCorte de cabello y recorte de puntas\nHaircut and split-end trim"],
            embedder.documents,
        )
        self.assertNotEqual(
            result.embeddings[0].contentChecksum,
            canonical_content_checksum("es", "Corte de cabello"),
        )

    def test_query_rejects_localized_document_fields(self) -> None:
        now = datetime.now(UTC)
        with self.assertRaisesRegex(ValidationError, "cannot contain localized"):
            EmbeddingBatchRequest.model_validate(
                {
                    "requestId": str(uuid4()),
                    "subjects": [
                        {
                            "subjectId": str(uuid4()),
                            "subjectType": "query",
                            "locale": "es",
                            "text": "corte",
                            "localizedTexts": {"es": "corte", "en": "haircut"},
                            "validFrom": now,
                            "expiresAt": now + timedelta(minutes=5),
                        }
                    ],
                }
            )

    def test_v1_raw_mode_rejects_localized_document_fields(self) -> None:
        now = datetime.now(UTC)
        request = EmbeddingBatchRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "subjects": [
                    {
                        "subjectId": str(uuid4()),
                        "subjectType": "service",
                        "locale": "es",
                        "text": "corte",
                        "localizedTexts": {"es": "corte", "en": "haircut"},
                        "validFrom": now,
                    }
                ],
            }
        )
        with self.assertRaisesRegex(ValueError, "LOCALIZED_TEXT_MODE_DISABLED"):
            EmbeddingBatchProcessor(_FakeEmbedder(), "multilingual-e5-small-v1").generate(
                request
            )


if __name__ == "__main__":
    unittest.main()
