"""Descubrimiento batch de atributos con embeddings, UMAP, HDBSCAN, BERTopic y c-TF-IDF."""

from __future__ import annotations

import hashlib
import importlib.metadata
import json
import math
import os
import re
import tempfile
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Literal
from uuid import UUID

import numpy as np
from pydantic import Field, model_validator

from .contracts import StrictContract, Version
from .embeddings import TextEmbedder


class AttributeDiscoveryPolicy(StrictContract):
    """Versiona componentes, hiperparámetros, fuentes y barreras de privacidad/revisión."""

    schemaVersion: Literal[1]
    policyVersion: Version
    ontologyVersion: Version
    embeddingModelVersion: Version
    embeddingDimensions: int = Field(ge=2, le=4096)
    umapVersion: str
    hdbscanVersion: str
    bertopicVersion: str
    randomSeed: int
    umapNeighbors: int = Field(ge=2, le=100)
    umapComponents: int = Field(ge=2, le=20)
    umapMinimumDistance: float = Field(ge=0, le=1)
    minimumClusterSize: int = Field(ge=5)
    minimumSamples: int = Field(ge=1)
    minimumDocumentsPerLocale: int = Field(ge=1)
    maximumDocuments: int = Field(ge=10, le=100_000)
    topTerms: int = Field(ge=3, le=20)
    allowedSources: list[Literal["verifiedReview", "venueDescription", "searchAggregate"]]
    stopWords: list[str] = Field(min_length=1)
    prohibitedTerms: list[str] = Field(min_length=1)
    automaticPublicationAllowed: Literal[False]
    requiredReviewRole: Literal["ROLE_ADMIN"]

    @model_validator(mode="after")
    def validate_policy(self) -> "AttributeDiscoveryPolicy":
        if len(set(self.allowedSources)) != len(self.allowedSources):
            raise ValueError("ATTRIBUTE_DISCOVERY_POLICY_INVALID")
        if self.minimumDocumentsPerLocale * 2 > self.minimumClusterSize:
            raise ValueError("ATTRIBUTE_DISCOVERY_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "AttributeDiscoveryPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class DiscoveryDocument(StrictContract):
    """Texto desidentificado y consentido cuya identidad técnica solo aporta trazabilidad."""

    documentId: UUID
    locale: Literal["es", "en"]
    source: Literal["verifiedReview", "venueDescription", "searchAggregate"]
    text: str = Field(min_length=8, max_length=1000)


class AttributeDiscoveryDataset(StrictContract):
    """Dataset efímero gobernado; el artefacto resultante nunca contiene sus textos."""

    datasetVersion: Version
    ontologyVersion: Version
    embeddingModelVersion: Version
    productionEvidence: bool
    purpose: Literal["ontologyDiscovery"]
    containsPersonalData: Literal[False]
    piiScanPassed: Literal[True]
    sensitiveTermsRemoved: Literal[True]
    consentRevocationsApplied: Literal[True]
    documents: list[DiscoveryDocument] = Field(min_length=10)

    @model_validator(mode="after")
    def validate_dataset(self) -> "AttributeDiscoveryDataset":
        ids = [document.documentId for document in self.documents]
        if len(ids) != len(set(ids)):
            raise ValueError("ATTRIBUTE_DISCOVERY_DOCUMENT_DUPLICATED")
        return self


class AttributeCandidate(StrictContract):
    """Cluster candidato minimizado; no es un atributo publicado ni una decisión automática."""

    candidateKey: Version
    clusterId: int = Field(ge=0)
    documentCount: int = Field(ge=1)
    documentsByLocale: dict[Literal["es", "en"], int]
    sources: list[Literal["verifiedReview", "venueDescription", "searchAggregate"]]
    representativeTerms: list[str] = Field(min_length=1)
    ctfidfScores: dict[str, float]
    meanMembershipProbability: float = Field(ge=0, le=1)
    possibleExistingAttributeCodes: list[Version]
    evidenceDocumentIds: list[UUID] = Field(max_length=20)
    status: Literal["pendingHumanReview"]
    automaticPublicationAllowed: Literal[False]
    requiredReviewRole: Literal["ROLE_ADMIN"]
    permittedReviewActions: list[Literal["name", "merge", "reject", "publish"]]


class AttributeDiscoveryResult(StrictContract):
    """Resultado reproducible y no publicable con versiones y conteo de ruido."""

    policyVersion: Version
    datasetVersion: Version
    ontologyVersion: Version
    embeddingModelVersion: Version
    libraryVersions: dict[Literal["umap-learn", "hdbscan", "bertopic"], str]
    componentsExecuted: list[Literal["embeddings", "UMAP", "HDBSCAN", "BERTopic", "c-TF-IDF"]]
    documentCount: int
    noiseDocumentCount: int
    candidates: list[AttributeCandidate]
    productionEvidence: bool
    automaticPublicationAllowed: Literal[False]


class AttributeDiscoveryPipeline:
    """Ejecuta clustering real offline y entrega únicamente candidatos para la cola humana V48."""

    def __init__(self, policy: AttributeDiscoveryPolicy, ontology_path: Path) -> None:
        self.policy = policy
        ontology = json.loads(ontology_path.read_text(encoding="utf-8"))
        if ontology.get("ontologyVersion") != policy.ontologyVersion:
            raise ValueError("ATTRIBUTE_DISCOVERY_ONTOLOGY_VERSION_MISMATCH")
        self._existing_attributes = {
            item["code"]: {_normalize(item["name"]["es"]), _normalize(item["name"]["en"])}
            for item in ontology["attributes"]
        }

    def discover(
        self, dataset: AttributeDiscoveryDataset, embedder: TextEmbedder
    ) -> AttributeDiscoveryResult:
        """Valida, embebe y agrupa; no persiste texto, publica ontología ni ejecuta ranking."""
        self._validate_input(dataset, embedder)
        UMAP, HDBSCAN, BERTopic, CountVectorizer, ClassTfidfTransformer = _load_ml_stack(
            self.policy
        )
        texts = [document.text.strip() for document in dataset.documents]
        vectors = np.asarray(embedder.encode_documents(texts), dtype=np.float32)
        if vectors.shape != (len(texts), self.policy.embeddingDimensions) or not np.isfinite(vectors).all():
            raise ValueError("ATTRIBUTE_DISCOVERY_EMBEDDING_INVALID")
        umap_model = UMAP(
            n_neighbors=self.policy.umapNeighbors,
            n_components=self.policy.umapComponents,
            min_dist=self.policy.umapMinimumDistance,
            metric="cosine",
            random_state=self.policy.randomSeed,
            n_jobs=1,
        )
        hdbscan_model = HDBSCAN(
            min_cluster_size=self.policy.minimumClusterSize,
            min_samples=self.policy.minimumSamples,
            metric="euclidean",
            cluster_selection_method="eom",
            prediction_data=True,
            core_dist_n_jobs=1,
        )
        vectorizer = CountVectorizer(
            lowercase=True,
            strip_accents="unicode",
            stop_words=self.policy.stopWords,
            ngram_range=(1, 2),
            # BERTopic agrega cada cluster como un documento antes de c-TF-IDF; `min_df=2`
            # eliminaría precisamente los términos discriminantes que aparecen en un solo cluster.
            min_df=1,
        )
        ctfidf = ClassTfidfTransformer(reduce_frequent_words=True)
        topic_model = BERTopic(
            language="multilingual",
            top_n_words=self.policy.topTerms,
            embedding_model=None,
            umap_model=umap_model,
            hdbscan_model=hdbscan_model,
            vectorizer_model=vectorizer,
            ctfidf_model=ctfidf,
            calculate_probabilities=False,
            verbose=False,
        )
        topics, _ = topic_model.fit_transform(texts, embeddings=vectors)
        memberships = getattr(topic_model.hdbscan_model, "probabilities_", np.ones(len(texts)))
        candidates = self._candidates(dataset, topic_model, topics, memberships)
        return AttributeDiscoveryResult(
            policyVersion=self.policy.policyVersion,
            datasetVersion=dataset.datasetVersion,
            ontologyVersion=dataset.ontologyVersion,
            embeddingModelVersion=dataset.embeddingModelVersion,
            libraryVersions={
                "umap-learn": importlib.metadata.version("umap-learn"),
                "hdbscan": importlib.metadata.version("hdbscan"),
                "bertopic": importlib.metadata.version("bertopic"),
            },
            componentsExecuted=["embeddings", "UMAP", "HDBSCAN", "BERTopic", "c-TF-IDF"],
            documentCount=len(texts),
            noiseDocumentCount=sum(topic == -1 for topic in topics),
            candidates=candidates,
            productionEvidence=dataset.productionEvidence,
            automaticPublicationAllowed=False,
        )

    def _validate_input(self, dataset: AttributeDiscoveryDataset, embedder: TextEmbedder) -> None:
        if (
            dataset.ontologyVersion != self.policy.ontologyVersion
            or dataset.embeddingModelVersion != self.policy.embeddingModelVersion
            or embedder.dimensions != self.policy.embeddingDimensions
            or len(dataset.documents) > self.policy.maximumDocuments
        ):
            raise ValueError("ATTRIBUTE_DISCOVERY_VERSION_OR_SIZE_MISMATCH")
        allowed = set(self.policy.allowedSources)
        prohibited = {_normalize(term) for term in self.policy.prohibitedTerms}
        for document in dataset.documents:
            normalized = _normalize(document.text)
            if document.source not in allowed or _contains_pii(document.text):
                raise ValueError("ATTRIBUTE_DISCOVERY_PRIVACY_REJECTED")
            if any(term in normalized.split() for term in prohibited):
                raise ValueError("ATTRIBUTE_DISCOVERY_SENSITIVE_TERM_REJECTED")

    def _candidates(self, dataset, topic_model, topics, memberships) -> list[AttributeCandidate]:
        result = []
        for topic in sorted(set(topics) - {-1}):
            indexes = [index for index, value in enumerate(topics) if value == topic]
            locales = Counter(dataset.documents[index].locale for index in indexes)
            if any(locales.get(locale, 0) < self.policy.minimumDocumentsPerLocale for locale in ("es", "en")):
                continue
            terms_with_scores = topic_model.get_topic(topic) or []
            terms = [term for term, _ in terms_with_scores[: self.policy.topTerms]]
            if not terms:
                continue
            key_digest = hashlib.sha256(
                f"{self.policy.policyVersion}|{'|'.join(sorted(terms))}".encode("utf-8")
            ).hexdigest()[:20]
            existing = self._existing_hints(terms)
            result.append(
                AttributeCandidate(
                    candidateKey=f"candidate-{key_digest}",
                    clusterId=topic,
                    documentCount=len(indexes),
                    documentsByLocale={"es": locales["es"], "en": locales["en"]},
                    sources=sorted({dataset.documents[index].source for index in indexes}),
                    representativeTerms=terms,
                    ctfidfScores={term: round(float(score), 8) for term, score in terms_with_scores[: self.policy.topTerms]},
                    meanMembershipProbability=round(sum(float(memberships[index]) for index in indexes) / len(indexes), 8),
                    possibleExistingAttributeCodes=existing,
                    evidenceDocumentIds=[dataset.documents[index].documentId for index in indexes[:20]],
                    status="pendingHumanReview",
                    automaticPublicationAllowed=False,
                    requiredReviewRole="ROLE_ADMIN",
                    permittedReviewActions=["name", "merge", "reject", "publish"],
                )
            )
        return sorted(result, key=lambda item: item.candidateKey)

    def _existing_hints(self, terms: list[str]) -> list[str]:
        term_tokens = set(_normalize(" ".join(terms)).split())
        return sorted(
            code
            for code, names in self._existing_attributes.items()
            if any(term_tokens & set(name.split()) for name in names)
        )


def _load_ml_stack(policy: AttributeDiscoveryPolicy):
    """Importa el stack batch con caché Numba temporal explícita y verifica supply chain."""
    cache = Path(tempfile.gettempdir()) / "reserly-numba-cache"
    cache.mkdir(parents=True, exist_ok=True)
    os.environ.setdefault("NUMBA_CACHE_DIR", str(cache))
    versions = {
        "umap-learn": policy.umapVersion,
        "hdbscan": policy.hdbscanVersion,
        "bertopic": policy.bertopicVersion,
    }
    if any(importlib.metadata.version(name) != version for name, version in versions.items()):
        raise ValueError("ATTRIBUTE_DISCOVERY_DEPENDENCY_VERSION_MISMATCH")
    from bertopic import BERTopic
    from bertopic.vectorizers import ClassTfidfTransformer
    from hdbscan import HDBSCAN
    from sklearn.feature_extraction.text import CountVectorizer
    from umap import UMAP

    return UMAP, HDBSCAN, BERTopic, CountVectorizer, ClassTfidfTransformer


def _normalize(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value.casefold())
    return " ".join(
        "".join(character for character in token if not unicodedata.combining(character))
        for token in re.findall(r"[\w]+", decomposed, flags=re.UNICODE)
    )


def _contains_pii(value: str) -> bool:
    return bool(
        re.search(r"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}", value, re.IGNORECASE)
        or re.search(r"(?:\+?\d[\s().-]*){9,}", value)
    )
