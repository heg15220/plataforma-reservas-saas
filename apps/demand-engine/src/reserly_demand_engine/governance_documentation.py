"""Valida documentación de modelos, datasets y privacidad como una puerta versionada.

La puerta solo procesa metadatos del repositorio. No carga datasets, no registra modelos y nunca
autoriza una promoción: su resultado demuestra cobertura documental, no aprobación jurídica.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from datetime import date, datetime
from pathlib import Path
from typing import Literal

from pydantic import Field, field_validator, model_validator

from .contracts import StrictContract, Version


class LocalizedText(StrictContract):
    """Texto documental obligatorio en los dos idiomas soportados."""

    es: str = Field(min_length=10, max_length=500)
    en: str = Field(min_length=10, max_length=500)


class DocumentationPolicy(StrictContract):
    """Contrato de cobertura que impide omitir una familia documental silenciosamente."""

    schemaVersion: Literal[1]
    policyVersion: Version
    requiredLocales: list[Literal["es", "en"]]
    requiredModelCardFields: list[Version] = Field(min_length=8)
    requiredDataSheetSections: list[Version] = Field(min_length=10)
    privacyImpactRequired: Literal[True]
    prohibitedMatrixRequired: Literal[True]
    humanApprovalRequired: Literal[True]
    automaticPromotionAllowed: Literal[False]

    @model_validator(mode="after")
    def supported_locales(self) -> "DocumentationPolicy":
        if set(self.requiredLocales) != {"es", "en"}:
            raise ValueError("GOVERNANCE_DOCUMENTATION_LOCALES_INVALID")
        return self


class DataSheet(StrictContract):
    """Ficha minimizada de un dataset lógico; describe datos pero no incluye filas."""

    datasetKey: Version
    datasetVersion: Version
    purpose: LocalizedText
    owner: Version
    sources: list[Version] = Field(min_length=1, max_length=16)
    fields: list[Version] = Field(min_length=1, max_length=128)
    population: str = Field(min_length=20, max_length=500)
    timeCoverage: str = Field(min_length=20, max_length=500)
    quality: list[Version] = Field(min_length=3, max_length=32)
    privacy: str = Field(min_length=20, max_length=500)
    retention: str = Field(min_length=10, max_length=300)
    knownLimitations: list[str] = Field(min_length=1, max_length=16)
    prohibitedUses: list[Version] = Field(min_length=1, max_length=32)

    @field_validator("sources", "fields", "quality", "prohibitedUses")
    @classmethod
    def unique_tokens(cls, values: list[str]) -> list[str]:
        if len(values) != len(set(values)):
            raise ValueError("GOVERNANCE_DOCUMENTATION_DUPLICATE_TOKEN")
        return values


class DataSheetRegistry(StrictContract):
    """Registro versionado con cobertura de los tres planos de datos del motor."""

    schemaVersion: Literal[1]
    registryVersion: Version
    generatedAt: datetime
    dataSheets: list[DataSheet] = Field(min_length=3, max_length=32)

    @model_validator(mode="after")
    def unique_datasets(self) -> "DataSheetRegistry":
        if self.generatedAt.tzinfo is None or self.generatedAt.utcoffset() is None:
            raise ValueError("GOVERNANCE_DOCUMENTATION_TIMESTAMP_INVALID")
        keys = [(item.datasetKey, item.datasetVersion) for item in self.dataSheets]
        if len(keys) != len(set(keys)):
            raise ValueError("GOVERNANCE_DOCUMENTATION_DATASET_DUPLICATED")
        return self


class RiskControl(StrictContract):
    """Riesgo y controles técnicos trazables dentro de la evaluación de privacidad."""

    code: Version
    severity: Literal["low", "medium", "high", "critical"]
    controls: list[Version] = Field(min_length=2, max_length=16)


class PrivacyApproval(StrictContract):
    """Referencias de aprobación; permanecen nulas hasta las revisiones formales de 23.14."""

    privacy: Version | None
    legal: Version | None
    security: Version | None
    equity: Version | None


class PrivacyImpactAssessment(StrictContract):
    """Evaluación técnica previa que no suplanta la revisión legal o de seguridad."""

    schemaVersion: Literal[1]
    assessmentVersion: Version
    status: Literal["requires-legal-approval", "approved", "retired"]
    owner: Version
    scope: list[Version] = Field(min_length=4)
    purposesSeparated: Literal[True]
    lawfulBasisReviewRequired: Literal[True]
    dataSubjects: list[Version] = Field(min_length=1)
    dataCategories: list[Version] = Field(min_length=1)
    excludedCategories: list[Version] = Field(min_length=4)
    necessity: str = Field(min_length=50, max_length=1000)
    risks: list[RiskControl] = Field(min_length=4)
    rights: list[Version] = Field(min_length=6)
    internationalTransfers: str = Field(min_length=20, max_length=500)
    residualRisk: Version
    approval: PrivacyApproval
    automaticActivationAllowed: Literal[False]

    @model_validator(mode="after")
    def approval_matches_status(self) -> "PrivacyImpactAssessment":
        values = self.approval.model_dump().values()
        if self.status == "approved" and any(value is None for value in values):
            raise ValueError("GOVERNANCE_DOCUMENTATION_APPROVAL_INCOMPLETE")
        if self.status != "approved" and any(value is not None for value in values):
            raise ValueError("GOVERNANCE_DOCUMENTATION_PREMATURE_APPROVAL")
        return self


class ProhibitedAttribute(StrictContract):
    """Atributo que falla cerrado para todos sus usos enumerados."""

    code: Version
    category: Literal["directIdentifier", "tracking", "sensitiveInference"]
    uses: list[Version] = Field(min_length=1)


class ProhibitedAttributeMatrix(StrictContract):
    """Matriz deny-by-default cuya relajación exige una nueva versión y revisión humana."""

    schemaVersion: Literal[1]
    matrixVersion: Version
    effectiveFrom: date
    defaultDecision: Literal["deny"]
    prohibited: list[ProhibitedAttribute] = Field(min_length=10)
    exceptionsAllowed: Literal[False]
    changeRequires: list[Version] = Field(min_length=5)
    automaticRelaxationAllowed: Literal[False]

    @model_validator(mode="after")
    def required_categories(self) -> "ProhibitedAttributeMatrix":
        categories = {item.category for item in self.prohibited}
        codes = {item.code.casefold() for item in self.prohibited}
        if categories != {"directIdentifier", "tracking", "sensitiveInference"}:
            raise ValueError("GOVERNANCE_DOCUMENTATION_PROHIBITED_CATEGORY_MISSING")
        if len(codes) != len(self.prohibited):
            raise ValueError("GOVERNANCE_DOCUMENTATION_PROHIBITED_DUPLICATED")
        return self


class DocumentationEvidence(StrictContract):
    """Resultado content-addressed; deliberadamente no concede promoción o activación."""

    policyVersion: Version
    modelCardCount: int = Field(gt=0)
    dataSheetCount: int = Field(ge=3)
    modelCardSha256: dict[Version, str]
    dataSheetsSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    privacyImpactSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    prohibitedMatrixSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    documentationComplete: Literal[True]
    legalApprovalRequired: Literal[True]
    promotionAuthorized: Literal[False]


def _load(path: Path, contract):
    """Carga JSON estricto desde una ruta explícita sin resolver contenido remoto."""
    return contract.model_validate_json(path.read_text(encoding="utf-8"))


def validate_governance_documentation(root: Path) -> DocumentationEvidence:
    """Valida cobertura, consistencia y ausencia de atributos prohibidos en model cards/policies."""
    governance = root / "governance"
    policy = _load(governance / "documentation-policy.v1.json", DocumentationPolicy)
    sheets_path = governance / "data-sheets.v1.json"
    pia_path = governance / "privacy-impact-assessment.v1.json"
    matrix_path = governance / "prohibited-attributes.v1.json"
    sheets = _load(sheets_path, DataSheetRegistry)
    pia = _load(pia_path, PrivacyImpactAssessment)
    matrix = _load(matrix_path, ProhibitedAttributeMatrix)

    prohibited = {item.code.casefold() for item in matrix.prohibited}
    digests: dict[str, str] = {}
    card_paths = sorted((root / "models").glob("*.model-card.json"))
    if not card_paths:
        raise ValueError("GOVERNANCE_DOCUMENTATION_MODEL_CARDS_MISSING")
    for card_path in card_paths:
        raw = json.loads(card_path.read_text(encoding="utf-8"))
        missing = set(policy.requiredModelCardFields) - set(raw)
        if missing or raw.get("humanApprovalRequired") is not True:
            raise ValueError("GOVERNANCE_DOCUMENTATION_MODEL_CARD_INCOMPLETE")
        values = {str(value).casefold() for value in raw.get("featureCodes", [])}
        if values & prohibited:
            raise ValueError("GOVERNANCE_DOCUMENTATION_PROHIBITED_MODEL_FEATURE")
        digests[card_path.name] = hashlib.sha256(card_path.read_bytes()).hexdigest()

    for feature_path in sorted((root / "policies").glob("*.json")):
        raw = json.loads(feature_path.read_text(encoding="utf-8"))
        values = {str(value).casefold() for value in raw.get("featureCodes", [])}
        if values & prohibited:
            raise ValueError("GOVERNANCE_DOCUMENTATION_PROHIBITED_POLICY_FEATURE")

    if pia.status == "approved":
        raise ValueError("GOVERNANCE_DOCUMENTATION_LEGAL_REVIEW_NOT_EXECUTED")
    return DocumentationEvidence(
        policyVersion=policy.policyVersion,
        modelCardCount=len(card_paths),
        dataSheetCount=len(sheets.dataSheets),
        modelCardSha256=digests,
        dataSheetsSha256=hashlib.sha256(sheets_path.read_bytes()).hexdigest(),
        privacyImpactSha256=hashlib.sha256(pia_path.read_bytes()).hexdigest(),
        prohibitedMatrixSha256=hashlib.sha256(matrix_path.read_bytes()).hexdigest(),
        documentationComplete=True,
        legalApprovalRequired=True,
        promotionAuthorized=False,
    )


def run() -> None:
    """CLI offline que devuelve únicamente evidencia agregada y hashes."""
    parser = argparse.ArgumentParser(description="Valida documentación gobernada del Demand Engine")
    parser.add_argument("--root", type=Path, required=True)
    arguments = parser.parse_args()
    print(validate_governance_documentation(arguments.root).model_dump_json())


if __name__ == "__main__":
    run()
