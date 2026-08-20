"""NLP léxico ES/EN gobernado para búsqueda de cuidado personal sin retener texto."""

from __future__ import annotations

import re
import unicodedata
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import Locale, RequestEnvelope, StrictContract, Version


EntityType = Literal["service", "availability", "attribute"]
Polarity = Literal["positive", "negative"]
_TOKEN = re.compile(r"[a-z0-9]+")
_EMAIL = re.compile(r"(?i)\b[^\s@]+@[^\s@]+\.[^\s@]+\b")
_PHONE_CANDIDATE = re.compile(r"(?<!\w)(?:\+?\d[\d\s().-]{7,}\d)(?!\w)")


def _normalize(value: str) -> str:
    """Normaliza Unicode, caja, diacríticos y espacios sin conservar una copia fuera del request."""
    folded = unicodedata.normalize("NFKD", unicodedata.normalize("NFKC", value).casefold())
    without_marks = "".join(char for char in folded if not unicodedata.combining(char))
    return " ".join(_TOKEN.findall(without_marks))


class NlpSynonym(StrictContract):
    """Concepto gobernado y sus expresiones equivalentes por idioma."""

    conceptCode: Version
    entityType: EntityType
    phrases: dict[Locale, list[str]]

    @model_validator(mode="after")
    def validate_phrases(self) -> "NlpSynonym":
        if set(self.phrases) != {"es", "en"}:
            raise ValueError("nlp synonym languages are incomplete")
        for values in self.phrases.values():
            normalized = [_normalize(value) for value in values]
            if not values or any(not value for value in normalized) or len(normalized) != len(set(normalized)):
                raise ValueError("nlp synonym phrases are invalid")
        return self


class NlpLabelRule(StrictContract):
    """Etiqueta multilabel y conceptos positivos que pueden activarla."""

    labelCode: Version
    conceptCodes: list[Version] = Field(min_length=1, max_length=20)


class PersonalCareNlpPolicy(StrictContract):
    """Política cerrada de normalización, negación, entidades y clasificación ES/EN."""

    schemaVersion: Literal[1]
    policyVersion: Version
    normalizationVersion: Version
    classificationVersion: Version
    negationWindowTokens: int = Field(ge=1, le=5)
    negators: dict[Locale, list[str]]
    prohibitedTerms: dict[Locale, list[str]]
    synonyms: list[NlpSynonym] = Field(min_length=1, max_length=100)
    labels: list[NlpLabelRule] = Field(min_length=1, max_length=20)

    @model_validator(mode="after")
    def validate_dictionary(self) -> "PersonalCareNlpPolicy":
        if set(self.negators) != {"es", "en"} or set(self.prohibitedTerms) != {"es", "en"}:
            raise ValueError("nlp policy languages are incomplete")
        for dictionary in (self.negators, self.prohibitedTerms):
            for values in dictionary.values():
                normalized = [_normalize(value) for value in values]
                if (
                    not values
                    or any(not value for value in normalized)
                    or len(normalized) != len(set(normalized))
                ):
                    raise ValueError("nlp policy dictionary entries are invalid")
        concepts = [item.conceptCode for item in self.synonyms]
        labels = [item.labelCode for item in self.labels]
        if len(concepts) != len(set(concepts)) or len(labels) != len(set(labels)):
            raise ValueError("nlp policy codes must be unique")
        known = set(concepts)
        if any(not set(rule.conceptCodes) <= known for rule in self.labels):
            raise ValueError("nlp label references an unknown concept")
        return self

    @classmethod
    def load(cls, path: Path) -> "PersonalCareNlpPolicy":
        """Carga el diccionario UTF-8 y rechaza drift de esquema o campos adicionales."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class NlpAnalyzeRequest(RequestEnvelope):
    """Texto efímero de búsqueda; solo está autorizado para el vertical de cuidado personal."""

    purpose: Literal["personalCareSearch"]
    text: str = Field(min_length=1, max_length=2000)

    @model_validator(mode="after")
    def reject_blank(self) -> "NlpAnalyzeRequest":
        if not self.text.strip():
            raise ValueError("nlp text cannot be blank")
        return self


class ExtractedEntity(StrictContract):
    """Concepto canónico sin fragmento, offsets ni texto de origen."""

    conceptCode: Version
    entityType: EntityType
    polarity: Polarity
    confidence: float = Field(ge=0, le=1)


class MultilabelPrediction(StrictContract):
    """Etiqueta interpretable sustentada únicamente por conceptos positivos observados."""

    labelCode: Version
    confidence: float = Field(ge=0, le=1)
    evidenceConceptCodes: list[Version] = Field(min_length=1, max_length=20)


class NlpAnalyzeResponse(StrictContract):
    """Resultado minimizado: nunca contiene texto original/normalizado ni identificadores personales."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    language: Locale
    normalizationVersion: Version
    classificationVersion: Version
    tokenCount: int = Field(ge=0, le=500)
    entities: list[ExtractedEntity] = Field(max_length=100)
    labels: list[MultilabelPrediction] = Field(max_length=20)


class PersonalCareNlpPipeline:
    """Aplica una allowlist léxica determinista, negación cercana y clasificación multilabel."""

    def __init__(self, policy: PersonalCareNlpPolicy) -> None:
        self.policy = policy

    def analyze(self, request: NlpAnalyzeRequest) -> NlpAnalyzeResponse:
        """Procesa el texto en memoria y devuelve solo conceptos gobernados; PII/salud fallan cerrado."""
        if request.policyVersion != self.policy.policyVersion:
            raise ValueError("NLP_POLICY_VERSION_MISMATCH")
        self._validate_safe_text(request.text, request.locale)
        tokens = _normalize(request.text).split()
        if not tokens or len(tokens) > 500:
            raise ValueError("NLP_TOKEN_LIMIT")
        entities = self._extract(tokens, request.locale)
        labels = self._classify(entities)
        return NlpAnalyzeResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            language=request.locale,
            normalizationVersion=self.policy.normalizationVersion,
            classificationVersion=self.policy.classificationVersion,
            tokenCount=len(tokens),
            entities=entities,
            labels=labels,
        )

    def _validate_safe_text(self, text: str, locale: Locale) -> None:
        if _EMAIL.search(text):
            raise ValueError("NLP_DIRECT_IDENTIFIER_REJECTED")
        for candidate in _PHONE_CANDIDATE.findall(text):
            if len(re.sub(r"\D", "", candidate)) >= 9:
                raise ValueError("NLP_DIRECT_IDENTIFIER_REJECTED")
        normalized = f" {_normalize(text)} "
        prohibited = self.policy.prohibitedTerms["es"] + self.policy.prohibitedTerms["en"]
        if any(f" {_normalize(term)} " in normalized for term in prohibited):
            raise ValueError("NLP_SENSITIVE_TERM_REJECTED")

    def _extract(self, tokens: list[str], locale: Locale) -> list[ExtractedEntity]:
        phrases: list[tuple[list[str], NlpSynonym]] = []
        for synonym in self.policy.synonyms:
            phrases.extend((_normalize(value).split(), synonym) for value in synonym.phrases[locale])
        phrases.sort(key=lambda item: (-len(item[0]), item[1].conceptCode, item[0]))
        occupied: set[int] = set()
        found: list[tuple[int, ExtractedEntity]] = []
        negators = {_normalize(value) for value in self.policy.negators[locale]}
        for index in range(len(tokens)):
            for phrase, synonym in phrases:
                end = index + len(phrase)
                if end > len(tokens) or any(position in occupied for position in range(index, end)):
                    continue
                if tokens[index:end] != phrase:
                    continue
                left = tokens[max(0, index - self.policy.negationWindowTokens) : index]
                polarity: Polarity = "negative" if any(token in negators for token in left) else "positive"
                found.append(
                    (
                        index,
                        ExtractedEntity(
                            conceptCode=synonym.conceptCode,
                            entityType=synonym.entityType,
                            polarity=polarity,
                            confidence=0.95,
                        ),
                    )
                )
                occupied.update(range(index, end))
                break
        return [entity for _, entity in sorted(found, key=lambda item: (item[0], item[1].conceptCode))]

    def _classify(self, entities: list[ExtractedEntity]) -> list[MultilabelPrediction]:
        positive = {item.conceptCode: item.confidence for item in entities if item.polarity == "positive"}
        predictions: list[MultilabelPrediction] = []
        for rule in self.policy.labels:
            evidence = sorted(set(rule.conceptCodes) & set(positive))
            if evidence:
                predictions.append(
                    MultilabelPrediction(
                        labelCode=rule.labelCode,
                        confidence=round(sum(positive[code] for code in evidence) / len(evidence), 8),
                        evidenceConceptCodes=evidence,
                    )
                )
        return sorted(predictions, key=lambda item: item.labelCode)
