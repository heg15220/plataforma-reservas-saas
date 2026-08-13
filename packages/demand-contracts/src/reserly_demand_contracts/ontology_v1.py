"""Contrato gobernado de la ontología v1 para cuidado personal con cita individual."""

from datetime import date
from enum import StrEnum
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, StringConstraints, model_validator


Code = Annotated[str, StringConstraints(pattern=r"^[a-z][a-zA-Z0-9]{1,63}$")]


class StrictOntologyModel(BaseModel):
    """Base inmutable sin extensiones ad hoc."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)


class AttributeFamily(StrEnum):
    """Familias aprobadas por diseño para el piloto."""

    AMBIENCE = "ambience"
    SPACE = "space"
    EXPERIENCE = "experience"
    OFFER = "offer"
    OPERATION = "operation"
    ACCESSIBILITY = "accessibility"


class AttributeType(StrEnum):
    """Semántica temporal y de evidencia del atributo."""

    STABLE = "stable"
    DYNAMIC = "dynamic"
    RELATIVE = "relative"
    SUBJECTIVE_AGGREGATE = "subjectiveAggregate"


class LocalizedText(StrictOntologyModel):
    """Texto obligatorio en los dos locales fundacionales."""

    es: Annotated[str, StringConstraints(min_length=3, max_length=240)]
    en: Annotated[str, StringConstraints(min_length=3, max_length=240)]


class FamilyDefinition(StrictOntologyModel):
    """Familia estable con presentación y alcance bilingüe."""

    code: AttributeFamily
    name: LocalizedText
    definition: LocalizedText


class SourceDefinition(StrictOntologyModel):
    """Fuente permitida y su alcance verificable."""

    code: Literal[
        "venueDeclaration",
        "structuredCatalog",
        "operational",
        "customerAggregate",
        "verifiedAudit",
        "imageAuxiliary",
    ]
    name: LocalizedText
    reliabilityClass: Literal["declared", "transactional", "aggregated", "verified", "auxiliary"]


class ValidityPolicy(StrictOntologyModel):
    """Vigencia estable hasta retirada o evidencia temporal con TTL."""

    mode: Literal["untilRetired", "ttl"]
    ttlDays: int | None = Field(default=None, ge=1, le=365)

    @model_validator(mode="after")
    def validate_mode(self) -> "ValidityPolicy":
        """Impide TTL fantasma o atributos temporales sin caducidad."""
        if (self.mode == "ttl") != (self.ttlDays is not None):
            raise ValueError("ttl mode and ttlDays must be supplied together")
        return self


class DemandAttributeDefinition(StrictOntologyModel):
    """Atributo publicable con jerarquía, fuentes, vigencia y usos cerrados."""

    code: Code
    family: AttributeFamily
    parentCode: Code | None = None
    name: LocalizedText
    definition: LocalizedText
    type: AttributeType
    allowedSources: Annotated[list[Code], Field(min_length=1, max_length=6)]
    validity: ValidityPolicy
    allowedUses: Annotated[
        list[Literal["profile", "filtering", "ranking", "explanation"]],
        Field(min_length=1, max_length=4),
    ]
    minimumEvidence: int = Field(ge=1, le=1000)
    status: Literal["published"]


class ProhibitedAttribute(StrictOntologyModel):
    """Inferencia que ningún productor puede proponer, extraer o usar."""

    code: Code
    category: Literal[
        "sensitive",
        "health",
        "demographic",
        "behavioralSurveillance",
        "unsupportedInference",
        "workerFairness",
    ]
    reason: LocalizedText


class DemandOntologyV1(StrictOntologyModel):
    """Documento completo, autoconsistente y listo para sembrar en 19.13."""

    ontologyVersion: Literal["personal-care.v1"]
    verticalCode: Literal["personalCareIndividualAppointment"]
    effectiveFrom: date
    locales: Literal[["es", "en"]]
    families: Annotated[list[FamilyDefinition], Field(min_length=6, max_length=6)]
    sources: Annotated[list[SourceDefinition], Field(min_length=6, max_length=6)]
    attributes: Annotated[list[DemandAttributeDefinition], Field(min_length=30, max_length=50)]
    prohibitedAttributes: Annotated[list[ProhibitedAttribute], Field(min_length=10, max_length=100)]

    @model_validator(mode="after")
    def validate_governance(self) -> "DemandOntologyV1":
        """Valida unicidad, cobertura, jerarquía, fuentes y semántica temporal."""
        family_codes = [family.code for family in self.families]
        source_codes = [source.code for source in self.sources]
        attribute_codes = [attribute.code for attribute in self.attributes]
        prohibited_codes = [attribute.code for attribute in self.prohibitedAttributes]
        if len(set(family_codes)) != len(family_codes) or set(family_codes) != set(AttributeFamily):
            raise ValueError("families must be unique and complete")
        if len(set(source_codes)) != len(source_codes):
            raise ValueError("sources must be unique")
        if len(set(attribute_codes)) != len(attribute_codes):
            raise ValueError("attribute codes must be unique")
        if len(set(prohibited_codes)) != len(prohibited_codes):
            raise ValueError("prohibited codes must be unique")
        if set(attribute_codes) & set(prohibited_codes):
            raise ValueError("published and prohibited attributes cannot overlap")

        by_code = {attribute.code: attribute for attribute in self.attributes}
        for attribute in self.attributes:
            if not set(attribute.allowedSources) <= set(source_codes):
                raise ValueError(f"unknown source in {attribute.code}")
            if len(set(attribute.allowedSources)) != len(attribute.allowedSources):
                raise ValueError(f"duplicated source in {attribute.code}")
            if len(set(attribute.allowedUses)) != len(attribute.allowedUses):
                raise ValueError(f"duplicated use in {attribute.code}")
            if attribute.parentCode is not None:
                parent = by_code.get(attribute.parentCode)
                if parent is None or parent.family != attribute.family:
                    raise ValueError(f"invalid parent in {attribute.code}")
            if attribute.type == AttributeType.STABLE and attribute.validity.mode != "untilRetired":
                raise ValueError(f"stable attribute {attribute.code} cannot expire by ttl")
            if attribute.type != AttributeType.STABLE and attribute.validity.mode != "ttl":
                raise ValueError(f"temporal attribute {attribute.code} requires ttl")
            if (
                attribute.type == AttributeType.SUBJECTIVE_AGGREGATE
                and (
                    "customerAggregate" not in attribute.allowedSources
                    or attribute.minimumEvidence < 5
                )
            ):
                raise ValueError(f"subjective aggregate {attribute.code} needs aggregated evidence")
        self._validate_no_cycles(by_code)
        return self

    def _validate_no_cycles(self, by_code: dict[str, DemandAttributeDefinition]) -> None:
        """Recorre cada cadena parental y falla ante cualquier ciclo."""
        for attribute in self.attributes:
            seen = {attribute.code}
            parent_code = attribute.parentCode
            while parent_code is not None:
                if parent_code in seen:
                    raise ValueError(f"hierarchy cycle at {attribute.code}")
                seen.add(parent_code)
                parent_code = by_code[parent_code].parentCode
