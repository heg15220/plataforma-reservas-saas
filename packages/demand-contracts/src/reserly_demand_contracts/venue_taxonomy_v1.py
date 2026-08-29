"""Contrato de la taxonomía candidata de locales físicos de Reserly.

El catálogo separa navegación, tipo físico y compatibilidad histórica, y conserva
únicamente la información funcional necesaria para clasificar locales en producto.
"""

from __future__ import annotations

from datetime import date
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, StringConstraints, model_validator


Slug = Annotated[str, StringConstraints(pattern=r"^[a-z0-9]+(?:-[a-z0-9]+)*$", max_length=128)]
class StrictTaxonomyModel(BaseModel):
    """Base inmutable y fail-closed para evitar extensiones no gobernadas."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)


class LocalizedCandidateName(StrictTaxonomyModel):
    """Etiqueta española fuente y traducción inglesa pendiente o revisada."""

    es: Annotated[str, StringConstraints(min_length=2, max_length=160)]
    en: Annotated[str, StringConstraints(min_length=2, max_length=160)] | None


class LocalizedText(StrictTaxonomyModel):
    """Texto bilingüe obligatorio para familias aptas para navegación."""

    es: Annotated[str, StringConstraints(min_length=3, max_length=320)]
    en: Annotated[str, StringConstraints(min_length=3, max_length=320)]


class TaxonomySource(StrictTaxonomyModel):
    """Proveniencia content-addressed del libro importado sin incorporarlo al repositorio."""

    fileSha256: Annotated[str, StringConstraints(pattern=r"^[a-f0-9]{64}$")]
    sourceVersion: date
    recordCount: Literal[254]
    scopeEs: Annotated[str, StringConstraints(min_length=20, max_length=320)]


class VenueFamily(StrictTaxonomyModel):
    """Familia de navegación candidata, no activada automáticamente en producto."""

    code: Slug
    name: LocalizedText
    definition: LocalizedText
    governanceStatus: Literal["candidate"]


class VenueTypeCandidate(StrictTaxonomyModel):
    """Tipo físico importado con etiqueta, jerarquía y uso funcional."""

    sourceId: int = Field(ge=1, le=254)
    code: Slug
    familyCode: Slug
    subcategoryCode: Slug
    name: LocalizedCandidateName
    sourceSubcategoryEs: Annotated[str, StringConstraints(min_length=2, max_length=120)]
    useCode: Slug
    useLabelEs: Annotated[str, StringConstraints(min_length=2, max_length=120)]
    translationStatus: Literal["pendingHumanReview", "reviewed"]
    governanceStatus: Literal["candidate"]


class LegacyCompatibility(StrictTaxonomyModel):
    """Puente explícito que evita reinterpretar silenciosamente las ocho clases históricas."""

    legacyCategoryCode: Literal[
        "restaurante",
        "peluqueria",
        "campo-de-futbol",
        "pista-de-padel",
        "instalacion-municipal",
        "centro-deportivo",
        "centro-de-estetica",
        "otros",
    ]
    mappingKind: Literal["canonicalType", "operatorAttribute", "compositeRequiresReview"]
    targetTypeCodes: list[Slug] = Field(max_length=8)
    operatorTypeCode: Literal["public-municipal"] | None
    mappingStatus: Literal["exact", "partial", "requiresReclassification"]
    existingImagesReusableForDevelopment: Literal[True]
    existingImagesEligibleAsNewTest: Literal[False]
    humanRelabelReviewRequired: Literal[True]


class VenueTaxonomyV1(StrictTaxonomyModel):
    """Catálogo autoconsistente de 23 familias, 254 candidatos y ocho puentes legacy."""

    schemaVersion: Literal[1]
    taxonomyVersion: Literal["venue-taxonomy.v1"]
    effectiveFrom: date
    locales: Literal[["es", "en"]]
    activationStatus: Literal["candidateOnly"]
    source: TaxonomySource
    families: list[VenueFamily] = Field(min_length=23, max_length=23)
    types: list[VenueTypeCandidate] = Field(min_length=254, max_length=254)
    legacyCompatibility: list[LegacyCompatibility] = Field(min_length=8, max_length=8)

    @model_validator(mode="after")
    def validate_catalog(self) -> "VenueTaxonomyV1":
        """Valida cobertura, unicidad, referencias y excepciones no visuales."""
        family_codes = [item.code for item in self.families]
        type_codes = [item.code for item in self.types]
        source_ids = [item.sourceId for item in self.types]
        legacy_codes = [item.legacyCategoryCode for item in self.legacyCompatibility]
        if len(set(family_codes)) != 23:
            raise ValueError("VENUE_TAXONOMY_FAMILY_DUPLICATED")
        if len(set(type_codes)) != 254:
            raise ValueError("VENUE_TAXONOMY_TYPE_DUPLICATED")
        if sorted(source_ids) != list(range(1, 255)):
            raise ValueError("VENUE_TAXONOMY_SOURCE_IDS_INVALID")
        if any(item.familyCode not in set(family_codes) for item in self.types):
            raise ValueError("VENUE_TAXONOMY_FAMILY_UNKNOWN")
        if len(set(legacy_codes)) != 8:
            raise ValueError("VENUE_TAXONOMY_LEGACY_DUPLICATED")

        known_types = set(type_codes)
        by_legacy = {item.legacyCategoryCode: item for item in self.legacyCompatibility}
        for item in self.legacyCompatibility:
            if not set(item.targetTypeCodes) <= known_types:
                raise ValueError("VENUE_TAXONOMY_LEGACY_TARGET_UNKNOWN")
            if item.mappingKind == "canonicalType" and not item.targetTypeCodes:
                raise ValueError("VENUE_TAXONOMY_CANONICAL_TARGET_REQUIRED")
            if item.mappingKind == "operatorAttribute" and item.operatorTypeCode is None:
                raise ValueError("VENUE_TAXONOMY_OPERATOR_REQUIRED")
        municipal = by_legacy["instalacion-municipal"]
        other = by_legacy["otros"]
        if municipal.mappingKind != "operatorAttribute" or municipal.targetTypeCodes:
            raise ValueError("VENUE_TAXONOMY_MUNICIPAL_IS_NOT_A_PHYSICAL_TYPE")
        if other.mappingKind != "compositeRequiresReview" or other.mappingStatus != "requiresReclassification":
            raise ValueError("VENUE_TAXONOMY_OTHER_MUST_BE_RECLASSIFIED")
        return self
