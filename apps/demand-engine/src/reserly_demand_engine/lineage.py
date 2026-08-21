"""Linaje inmutable de datos, features, modelos, experimentos y promoción.

El manifiesto es un DAG content-addressed. Cada referencia padre incluye versión y SHA-256 exactos;
por tanto una ejecución puede reconstruir qué datos/configuración produjeron cada activo y una
decisión de promoción no puede desligarse del modelo y experimento evaluados. Solo se guardan
metadatos gobernados, nunca filas, vectores, texto libre de clientes ni secretos.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from datetime import date, datetime
from pathlib import Path
from typing import Annotated, Literal, Union

from pydantic import Field, field_validator, model_validator

from .contracts import StrictContract, Version


Sha256 = Annotated[str, Field(pattern=r"^[a-f0-9]{64}$")]
GitCommit = Annotated[str, Field(pattern=r"^[a-f0-9]{40}$")]
ArtifactType = Literal[
    "dataset",
    "featureSet",
    "ontology",
    "embedding",
    "configuration",
    "model",
    "experiment",
    "promotionDecision",
]
ModelStatus = Literal["candidate", "shadow", "canary", "champion", "retired"]


class ArtifactReference(StrictContract):
    """Referencia fuerte: nombre solo no basta para aceptar un padre mutable."""

    artifactId: Version
    version: Version
    sha256: Sha256


class ArtifactBase(StrictContract):
    """Metadatos comunes minimizados de cualquier activo versionado."""

    artifactId: Version
    artifactType: ArtifactType
    version: Version
    uri: str = Field(pattern=r"^(repo://[A-Za-z0-9._/-]+|s3://[^\s]+|mlflow-artifacts:/[^\s]+)$")
    sha256: Sha256
    createdAt: datetime
    producerGitCommit: GitCommit
    owner: Version
    purpose: str = Field(min_length=20, max_length=300)
    immutable: Literal[True]
    personalDataStatus: Literal["none", "synthetic", "pseudonymized", "aggregated"]
    parents: list[ArtifactReference] = Field(default_factory=list, max_length=16)

    @field_validator("createdAt")
    @classmethod
    def require_aware_created_at(cls, value: datetime) -> datetime:
        """Normaliza comparaciones y evita artefactos con zona temporal ambigua."""
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("LINEAGE_TIMESTAMP_MUST_INCLUDE_TIMEZONE")
        return value

    @model_validator(mode="after")
    def unique_parents(self) -> "ArtifactBase":
        keys = [(parent.artifactId, parent.version, parent.sha256) for parent in self.parents]
        if len(keys) != len(set(keys)):
            raise ValueError("LINEAGE_PARENT_DUPLICATED")
        return self


class DatasetArtifact(ArtifactBase):
    """Snapshot o dataset lógico con corte, esquema y revocaciones verificables."""

    artifactType: Literal["dataset"]
    datasetSchemaVersion: Version
    cutoffAt: datetime
    rowCount: int = Field(ge=0)
    consentRevocationsApplied: Literal[True]
    directIdentifiersExcluded: Literal[True]


class FeatureSetArtifact(ArtifactBase):
    """Lista cerrada de features disponible point-in-time y sin leakage conocido."""

    artifactType: Literal["featureSet"]
    featureCodes: list[Version] = Field(min_length=1, max_length=128)
    pointInTimeCorrect: Literal[True]
    prohibitedFeaturesExcluded: Literal[True]

    @field_validator("featureCodes")
    @classmethod
    def unique_features(cls, values: list[str]) -> list[str]:
        if len(values) != len(set(values)):
            raise ValueError("LINEAGE_FEATURE_DUPLICATED")
        return values


class OntologyArtifact(ArtifactBase):
    """Ontología editorial efectiva y bilingüe sometida a revisión humana."""

    artifactType: Literal["ontology"]
    effectiveFrom: date
    locales: list[Literal["es", "en"]] = Field(min_length=2, max_length=2)
    humanReviewed: Literal[True]

    @field_validator("locales")
    @classmethod
    def require_supported_locales(cls, values: list[str]) -> list[str]:
        if set(values) != {"es", "en"}:
            raise ValueError("LINEAGE_ONTOLOGY_LOCALE_MISSING")
        return values


class EmbeddingArtifact(ArtifactBase):
    """Versión reproducible de modelo/proyección vectorial, nunca el vector completo."""

    artifactType: Literal["embedding"]
    repository: str = Field(min_length=3, max_length=200)
    modelRevision: GitCommit
    dimensions: int = Field(ge=8, le=65536)
    normalized: bool


class ConfigurationArtifact(ArtifactBase):
    """Política/configuración sin secretos que gobierna una ejecución reproducible."""

    artifactType: Literal["configuration"]
    configurationKind: Literal["training", "ranking", "evaluation", "orchestration"]
    secretsIncluded: Literal[False]


class ModelArtifact(ArtifactBase):
    """Modelo gobernado; el registro no equivale a promoción ni despliegue."""

    artifactType: Literal["model"]
    status: ModelStatus
    modelCardUri: str = Field(pattern=r"^repo://[A-Za-z0-9._/-]+$")
    humanApprovalRequired: Literal[True]
    automaticDeploymentAllowed: Literal[False]


class ExperimentArtifact(ArtifactBase):
    """Definición/ejecución experimental enlazada a población y política exactas."""

    artifactType: Literal["experiment"]
    protocolVersion: Version
    assignmentUnit: Version
    status: Literal["planned", "running", "stopped", "completed", "invalidated"]
    automaticActivationAllowed: Literal[False]


class PromotionDecisionArtifact(ArtifactBase):
    """Decisión humana auditable que conserva estado anterior y rollback explícito."""

    artifactType: Literal["promotionDecision"]
    decision: Literal["approved", "rejected", "deferred", "rolledBack"]
    fromStatus: ModelStatus
    toStatus: ModelStatus
    approvedBy: Version | None = None
    rollbackVersion: Version
    automaticPromotionAllowed: Literal[False]

    @model_validator(mode="after")
    def require_consistent_decision(self) -> "PromotionDecisionArtifact":
        if self.decision == "approved" and (self.approvedBy is None or self.fromStatus == self.toStatus):
            raise ValueError("LINEAGE_APPROVED_PROMOTION_INVALID")
        if self.decision in {"rejected", "deferred"} and self.fromStatus != self.toStatus:
            raise ValueError("LINEAGE_NON_APPROVED_STATUS_CHANGE")
        return self


Artifact = Annotated[
    Union[
        DatasetArtifact,
        FeatureSetArtifact,
        OntologyArtifact,
        EmbeddingArtifact,
        ConfigurationArtifact,
        ModelArtifact,
        ExperimentArtifact,
        PromotionDecisionArtifact,
    ],
    Field(discriminator="artifactType"),
]


class LineageManifest(StrictContract):
    """DAG autocontenido con cobertura completa y referencias content-addressed."""

    schemaVersion: Literal[1]
    manifestVersion: Version
    generatedAt: datetime
    sourceGitCommit: GitCommit
    environment: Literal["local", "staging", "production"]
    artifacts: list[Artifact] = Field(min_length=8, max_length=100)

    @model_validator(mode="after")
    def validate_graph(self) -> "LineageManifest":
        by_id = {artifact.artifactId: artifact for artifact in self.artifacts}
        if len(by_id) != len(self.artifacts):
            raise ValueError("LINEAGE_ARTIFACT_ID_DUPLICATED")
        versions = [(artifact.artifactType, artifact.version) for artifact in self.artifacts]
        if len(versions) != len(set(versions)):
            raise ValueError("LINEAGE_TYPE_VERSION_DUPLICATED")
        required_types = set(ArtifactType.__args__)
        if {artifact.artifactType for artifact in self.artifacts} != required_types:
            raise ValueError("LINEAGE_ARTIFACT_TYPE_COVERAGE_INVALID")

        parent_types: dict[str, set[str]] = {}
        edges: dict[str, list[str]] = {}
        for artifact in self.artifacts:
            edges[artifact.artifactId] = []
            parent_types[artifact.artifactId] = set()
            for reference in artifact.parents:
                parent = by_id.get(reference.artifactId)
                if parent is None:
                    raise ValueError("LINEAGE_PARENT_MISSING")
                if reference.version != parent.version or reference.sha256 != parent.sha256:
                    raise ValueError("LINEAGE_PARENT_INTEGRITY_MISMATCH")
                if parent.artifactId == artifact.artifactId:
                    raise ValueError("LINEAGE_SELF_REFERENCE")
                edges[artifact.artifactId].append(parent.artifactId)
                parent_types[artifact.artifactId].add(parent.artifactType)

        required_parents: dict[str, set[str]] = {
            "featureSet": {"dataset", "ontology", "configuration"},
            "embedding": {"dataset", "ontology", "configuration"},
            "model": {"dataset", "featureSet", "ontology", "embedding", "configuration"},
            "experiment": {"dataset", "model", "configuration"},
            "promotionDecision": {"model", "experiment", "configuration"},
        }
        for artifact in self.artifacts:
            missing = required_parents.get(artifact.artifactType, set()) - parent_types[artifact.artifactId]
            if missing:
                raise ValueError(f"LINEAGE_REQUIRED_PARENT_MISSING:{artifact.artifactType}")

        visiting: set[str] = set()
        visited: set[str] = set()

        def visit(artifact_id: str) -> None:
            if artifact_id in visiting:
                raise ValueError("LINEAGE_CYCLE_DETECTED")
            if artifact_id in visited:
                return
            visiting.add(artifact_id)
            for parent_id in edges[artifact_id]:
                visit(parent_id)
            visiting.remove(artifact_id)
            visited.add(artifact_id)

        for artifact_id in edges:
            visit(artifact_id)
        return self

    @classmethod
    def load(cls, path: Path) -> "LineageManifest":
        """Carga JSON estricto; cualquier campo o tipo desconocido falla antes de operar."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))

    def verify_repository_artifacts(self, repository_root: Path) -> None:
        """Verifica SHA-256 de URI repo:// sin permitir escapes fuera del repositorio."""
        root = repository_root.resolve()
        for artifact in self.artifacts:
            if not artifact.uri.startswith("repo://"):
                continue
            relative = artifact.uri.removeprefix("repo://")
            target = (root / relative).resolve()
            if root not in target.parents or not target.is_file():
                raise ValueError("LINEAGE_REPOSITORY_URI_INVALID")
            digest = hashlib.sha256(target.read_bytes()).hexdigest()
            if digest != artifact.sha256:
                raise ValueError(f"LINEAGE_ARTIFACT_DIGEST_MISMATCH:{artifact.artifactId}")

    def digest(self) -> str:
        """Calcula identidad canónica del manifiesto completo para tags y auditoría."""
        payload = json.dumps(
            self.model_dump(mode="json"), sort_keys=True, separators=(",", ":"), ensure_ascii=False
        ).encode("utf-8")
        return hashlib.sha256(payload).hexdigest()

    def mlflow_tags(self) -> dict[str, str]:
        """Proyecta versiones/digests a MLflow sin enviar contenido de los activos."""
        tags = {
            "reserly.lineage.manifestVersion": self.manifestVersion,
            "reserly.lineage.manifestSha256": self.digest(),
            "reserly.lineage.sourceGitCommit": self.sourceGitCommit,
        }
        for artifact in self.artifacts:
            prefix = f"reserly.lineage.{artifact.artifactType}"
            tags[f"{prefix}.version"] = artifact.version
            tags[f"{prefix}.sha256"] = artifact.sha256
        return tags


def run() -> None:
    """CLI offline: valida grafo y ficheros; no registra ni promueve nada."""
    parser = argparse.ArgumentParser(description="Valida un manifiesto de linaje Reserly")
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--repository-root", type=Path, required=True)
    arguments = parser.parse_args()
    manifest = LineageManifest.load(arguments.manifest)
    manifest.verify_repository_artifacts(arguments.repository_root)
    print(json.dumps({"manifestVersion": manifest.manifestVersion, "sha256": manifest.digest()}))


if __name__ == "__main__":
    run()
