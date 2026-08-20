"""ABSA ES/EN sobre reseñas verificadas, con salida minimizada y evaluación humana."""

from __future__ import annotations

import re
import unicodedata
from datetime import datetime, timedelta
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import Locale, RequestEnvelope, StrictContract, Version


_TOKEN = re.compile(r"[a-z0-9]+")


def _normalize(value: str) -> list[str]:
    """Normaliza texto solo en memoria y devuelve tokens sin conservar fragmentos."""
    folded = unicodedata.normalize("NFKD", unicodedata.normalize("NFKC", value).casefold())
    plain = "".join(char for char in folded if not unicodedata.combining(char))
    return _TOKEN.findall(plain)


class HumanEvaluationGate(StrictContract):
    """Umbrales previos a usar los scores como evidencia agregada."""

    minimumReviewedReviews: int = Field(ge=10, le=100_000)
    minimumPolarityAccuracy: float = Field(ge=0, le=1)
    maximumMacroMae: float = Field(ge=0, le=2)


class AbsaAspectPolicy(StrictContract):
    """Aspecto publicado y expresiones que pueden mencionarlo en cada idioma."""

    aspectCode: Version
    validityDays: int = Field(ge=1, le=365)
    terms: dict[Locale, list[str]]


class ReviewAbsaPolicy(StrictContract):
    """Política cerrada de aspectos, sentimiento, negación, vigencia y revisión."""

    schemaVersion: Literal[1]
    policyVersion: Version
    extractorVersion: Version
    sentimentWindowTokens: int = Field(ge=1, le=8)
    negationWindowTokens: int = Field(ge=1, le=4)
    minimumAutoAcceptConfidence: float = Field(ge=0, le=1)
    humanEvaluation: HumanEvaluationGate
    negators: dict[Locale, list[str]]
    positiveTerms: dict[Locale, list[str]]
    negativeTerms: dict[Locale, list[str]]
    aspects: list[AbsaAspectPolicy] = Field(min_length=1, max_length=20)

    @model_validator(mode="after")
    def validate_dictionary(self) -> "ReviewAbsaPolicy":
        dictionaries = [self.negators, self.positiveTerms, self.negativeTerms]
        if any(set(item) != {"es", "en"} for item in dictionaries):
            raise ValueError("ABSA_LANGUAGES_INCOMPLETE")
        codes = [item.aspectCode for item in self.aspects]
        if len(codes) != len(set(codes)) or any(set(item.terms) != {"es", "en"} for item in self.aspects):
            raise ValueError("ABSA_ASPECT_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "ReviewAbsaPolicy":
        """Carga una versión inmutable y rechaza campos desconocidos."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class VerifiedReviewRequest(RequestEnvelope):
    """Reseña acreditada por Spring; email, cliente y reserva no cruzan esta frontera."""

    reviewId: UUID
    venueId: UUID
    verifiedReservation: Literal[True]
    rating: int = Field(ge=1, le=5)
    text: str = Field(min_length=1, max_length=2000)


class AspectSentiment(StrictContract):
    """Score independiente por aspecto, con confianza y vigencia explícitas."""

    aspectCode: Version
    score: float = Field(ge=-1, le=1)
    confidence: float = Field(ge=0, le=1)
    evidenceCount: int = Field(ge=1, le=50)
    observedAt: datetime
    expiresAt: datetime
    reviewStatus: Literal["machineAccepted", "pendingHuman"]


class ReviewAbsaResponse(StrictContract):
    """Derivados por reseña sin comentario, fragmentos, identidad o valoración global inferida."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    reviewId: UUID
    venueId: UUID
    policyVersion: Version
    extractorVersion: Version
    language: Locale
    aspects: list[AspectSentiment] = Field(max_length=20)


class HumanAspectLabel(StrictContract):
    """Juicio humano independiente usado solo para evaluar un aspecto mencionado."""

    aspectCode: Version
    score: Literal[-1, 0, 1]


class HumanReviewedCase(StrictContract):
    """Par minimizado de predicción y etiqueta; no contiene texto ni identidad."""

    reviewId: UUID
    predicted: list[AspectSentiment] = Field(min_length=1, max_length=20)
    humanLabels: list[HumanAspectLabel] = Field(min_length=1, max_length=20)


class AbsaEvaluationRequest(RequestEnvelope):
    """Cohorte humana versionada para decidir si el extractor puede promoverse."""

    datasetVersion: Version
    cases: list[HumanReviewedCase] = Field(min_length=1, max_length=10_000)


class AbsaEvaluationResponse(StrictContract):
    """Métricas agregadas de evaluación humana sin ejemplos ni UUID de reseña."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    extractorVersion: Version
    datasetVersion: Version
    reviewedReviews: int
    comparedAspects: int
    polarityAccuracy: float
    macroMae: float
    promotable: bool


class ReviewAbsaAnalyzer:
    """Extrae sentimiento local por aspecto y evalúa el baseline contra etiquetas humanas."""

    def __init__(self, policy: ReviewAbsaPolicy) -> None:
        self.policy = policy

    def analyze(self, review: VerifiedReviewRequest) -> ReviewAbsaResponse:
        """Analiza una reseña verificada; el rating global no rellena aspectos ausentes."""
        self._require_policy(review.policyVersion)
        tokens = _normalize(review.text)
        results: list[AspectSentiment] = []
        for aspect in self.policy.aspects:
            sentiment = self._aspect_sentiment(tokens, review.locale, aspect)
            if sentiment is None:
                continue
            score, count, contradictory = sentiment
            confidence = min(0.5 + 0.1 * count, 0.9)
            status = (
                "pendingHuman"
                if contradictory or confidence < self.policy.minimumAutoAcceptConfidence
                else "machineAccepted"
            )
            results.append(
                AspectSentiment(
                    aspectCode=aspect.aspectCode,
                    score=round(score, 8),
                    confidence=round(confidence, 8),
                    evidenceCount=count,
                    observedAt=review.occurredAt,
                    expiresAt=review.occurredAt + timedelta(days=aspect.validityDays),
                    reviewStatus=status,
                )
            )
        return ReviewAbsaResponse(
            requestId=review.requestId,
            reviewId=review.reviewId,
            venueId=review.venueId,
            policyVersion=self.policy.policyVersion,
            extractorVersion=self.policy.extractorVersion,
            language=review.locale,
            aspects=sorted(results, key=lambda item: item.aspectCode),
        )

    def evaluate(self, request: AbsaEvaluationRequest) -> AbsaEvaluationResponse:
        """Compara solo intersecciones aspecto/reseña y aplica puertas humanas versionadas."""
        self._require_policy(request.policyVersion)
        absolute_errors: list[float] = []
        polarity_hits = 0
        for case in request.cases:
            predicted = {item.aspectCode: item.score for item in case.predicted}
            labels = {item.aspectCode: item.score for item in case.humanLabels}
            if len(predicted) != len(case.predicted) or len(labels) != len(case.humanLabels):
                raise ValueError("ABSA_DUPLICATE_ASPECT")
            common = set(predicted) & set(labels)
            if not common:
                raise ValueError("ABSA_HUMAN_LABEL_MISMATCH")
            for code in common:
                expected, observed = labels[code], predicted[code]
                absolute_errors.append(abs(expected - observed))
                polarity_hits += int(self._polarity(observed) == self._polarity(expected))
        compared = len(absolute_errors)
        accuracy = polarity_hits / compared
        mae = sum(absolute_errors) / compared
        gate = self.policy.humanEvaluation
        promotable = (
            len(request.cases) >= gate.minimumReviewedReviews
            and accuracy >= gate.minimumPolarityAccuracy
            and mae <= gate.maximumMacroMae
        )
        return AbsaEvaluationResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            extractorVersion=self.policy.extractorVersion,
            datasetVersion=request.datasetVersion,
            reviewedReviews=len(request.cases),
            comparedAspects=compared,
            polarityAccuracy=round(accuracy, 8),
            macroMae=round(mae, 8),
            promotable=promotable,
        )

    def _aspect_sentiment(
        self, tokens: list[str], locale: Locale, aspect: AbsaAspectPolicy
    ) -> tuple[float, int, bool] | None:
        mentions: list[tuple[int, int]] = []
        for term in aspect.terms[locale]:
            phrase = _normalize(term)
            for index in range(len(tokens) - len(phrase) + 1):
                if tokens[index : index + len(phrase)] == phrase:
                    mentions.append((index, index + len(phrase)))
        if not mentions:
            return None
        positive = [(_normalize(term), 1.0) for term in self.policy.positiveTerms[locale]]
        negative = [(_normalize(term), -1.0) for term in self.policy.negativeTerms[locale]]
        negators = set(self.policy.negators[locale])
        evidence: list[float] = []
        for start, end in mentions:
            left = max(0, start - self.policy.sentimentWindowTokens)
            right = min(len(tokens), end + self.policy.sentimentWindowTokens)
            for phrase, polarity in positive + negative:
                for index in range(left, right - len(phrase) + 1):
                    if tokens[index : index + len(phrase)] != phrase:
                        continue
                    negation_left = tokens[max(left, index - self.policy.negationWindowTokens) : index]
                    evidence.append(-polarity if any(token in negators for token in negation_left) else polarity)
        if not evidence:
            return None
        score = sum(evidence) / len(evidence)
        return score, len(evidence), min(evidence) < 0 < max(evidence)

    def _require_policy(self, version: str) -> None:
        if version != self.policy.policyVersion:
            raise ValueError("ABSA_POLICY_VERSION_MISMATCH")

    @staticmethod
    def _polarity(value: float) -> int:
        if value > 0.2:
            return 1
        if value < -0.2:
            return -1
        return 0
