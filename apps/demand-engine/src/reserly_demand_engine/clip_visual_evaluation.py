"""Evaluación de CLIP como evidencia visual auxiliar, no como fuente de afirmaciones sensibles."""

from __future__ import annotations

import math
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


class ClipVisualManifest(StrictContract):
    """Fija artefacto, dimensionalidad, allowlist visual y afirmaciones prohibidas."""

    manifestVersion: Literal[1]
    modelKey: Version
    repository: str
    revision: str = Field(pattern=r"^[0-9a-f]{40}$")
    license: Literal["MIT"]
    architecture: Literal["ViT-B/32"]
    library: Literal["transformers"]
    libraryVersion: Literal["4.56.2"]
    imageLibrary: Literal["Pillow"]
    imageLibraryVersion: Literal["11.3.0"]
    dimensions: Literal[512]
    normalizeEmbeddings: Literal[True]
    trustRemoteCode: Literal[False]
    allowedAttributeCodes: list[Version] = Field(min_length=1)
    prohibitedClaims: list[Version] = Field(min_length=1)
    auxiliaryEvidenceOnly: Literal[True]
    humanReviewRequired: Literal[True]

    @model_validator(mode="after")
    def validate_manifest(self) -> "ClipVisualManifest":
        if (
            len(self.allowedAttributeCodes) != len(set(self.allowedAttributeCodes))
            or len(self.prohibitedClaims) != len(set(self.prohibitedClaims))
            or set(self.allowedAttributeCodes) & set(self.prohibitedClaims)
        ):
            raise ValueError("CLIP_MANIFEST_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "ClipVisualManifest":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class HuggingFaceClipEmbedder:
    """Genera embeddings offline desde rutas controladas usando artefacto y revisión fijados.

    No forma parte del endpoint HTTP: los píxeles permanecen en el job autorizado. El caller debe
    acreditar licencia/consentimiento, retirar EXIF y excluir imágenes con personas antes de usar el
    vector en el evaluador.
    """

    def __init__(self, manifest: ClipVisualManifest, local_files_only: bool = True) -> None:
        self.manifest = manifest
        self.local_files_only = local_files_only
        self._model = None
        self._processor = None

    def encode_images(self, paths: list[Path]) -> list["NormalizedClipVector"]:
        """Codifica imágenes locales en CPU y cierra cada handle tras procesarlo."""
        if not paths:
            return []
        from PIL import Image
        import torch

        model, processor = self._load()
        images = []
        try:
            for path in paths:
                with Image.open(path) as source:
                    images.append(source.convert("RGB"))
            inputs = processor(images=images, return_tensors="pt")
            with torch.inference_mode():
                features = model.get_image_features(**inputs)
            return self._vectors(features)
        finally:
            for image in images:
                image.close()

    def encode_prompts(self, prompts: list[str]) -> list["NormalizedClipVector"]:
        """Codifica únicamente prompts revisados; no persiste el texto del prompt."""
        if not prompts:
            return []
        import torch

        model, processor = self._load()
        inputs = processor(text=prompts, return_tensors="pt", padding=True)
        with torch.inference_mode():
            features = model.get_text_features(**inputs)
        return self._vectors(features)

    def _load(self):
        if self._model is None or self._processor is None:
            from transformers import CLIPModel, CLIPProcessor

            self._processor = CLIPProcessor.from_pretrained(
                self.manifest.repository,
                revision=self.manifest.revision,
                local_files_only=self.local_files_only,
                trust_remote_code=self.manifest.trustRemoteCode,
                use_fast=False,
            )
            self._model = CLIPModel.from_pretrained(
                self.manifest.repository,
                revision=self.manifest.revision,
                local_files_only=self.local_files_only,
                trust_remote_code=self.manifest.trustRemoteCode,
                use_safetensors=True,
            )
            self._model.eval()
        return self._model, self._processor

    def _vectors(self, features) -> list["NormalizedClipVector"]:
        normalized = features / features.norm(dim=-1, keepdim=True).clamp_min(1e-12)
        return [
            NormalizedClipVector(
                modelKey=self.manifest.modelKey,
                modelRevision=self.manifest.revision,
                values=[float(value) for value in row.tolist()],
            )
            for row in normalized.cpu()
        ]


class ClipVisualPolicy(StrictContract):
    """Versiona muestra, umbrales, logits y prohibición de mutación automática."""

    schemaVersion: Literal[1]
    policyVersion: Version
    modelKey: Version
    minimumImages: int = Field(ge=20, le=100_000)
    similarityLogitScale: float = Field(gt=0, le=100)
    predictionThreshold: float = Field(gt=0.5, lt=1)
    minimumMacroPrecision: float = Field(ge=0, le=1)
    minimumMacroRecall: float = Field(ge=0, le=1)
    maximumEvidencePerImage: int = Field(ge=1, le=10)
    automaticProfileMutationAllowed: Literal[False]

    @classmethod
    def load(cls, path: Path) -> "ClipVisualPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class NormalizedClipVector(StrictContract):
    """Embedding CLIP L2 normalizado; imagen y prompts comparten revisión exacta."""

    modelKey: Version
    modelRevision: str = Field(pattern=r"^[0-9a-f]{40}$")
    values: list[float] = Field(min_length=512, max_length=512)

    @model_validator(mode="after")
    def validate_vector(self) -> "NormalizedClipVector":
        if not all(math.isfinite(value) for value in self.values):
            raise ValueError("CLIP_VECTOR_NON_FINITE")
        norm = math.sqrt(sum(value * value for value in self.values))
        if not 0.999 <= norm <= 1.001:
            raise ValueError("CLIP_VECTOR_NOT_NORMALIZED")
        return self


class ClipPromptPair(StrictContract):
    """Prompts positivo/negativo revisados fuera del dataset y sin texto libre en la respuesta."""

    attributeCode: Version
    positive: NormalizedClipVector
    negative: NormalizedClipVector


class ClipVisualImage(StrictContract):
    """Imagen autorizada representada solo por hash, embedding y etiqueta humana de evaluación."""

    imageId: UUID
    imageSha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    venueId: UUID
    capturedAt: datetime
    venueAuthorized: Literal[True]
    metadataStripped: Literal[True]
    peopleDetected: bool
    embedding: NormalizedClipVector
    humanVerifiedAttributes: list[Version] = Field(max_length=4)

    @model_validator(mode="after")
    def validate_image(self) -> "ClipVisualImage":
        if self.capturedAt.tzinfo is None or self.capturedAt.utcoffset() is None:
            raise ValueError("CLIP_IMAGE_TIME_INVALID")
        if len(self.humanVerifiedAttributes) != len(set(self.humanVerifiedAttributes)):
            raise ValueError("CLIP_LABEL_DUPLICATED")
        return self


class ClipVisualEvaluationRequest(RequestEnvelope):
    """Dataset cerrado de evaluación, sin píxeles, EXIF, personas ni identificadores directos."""

    datasetVersion: Version
    productionEvidence: bool
    containsPersonalData: Literal[False]
    requestedAttributeCodes: list[Version] = Field(min_length=1, max_length=4)
    promptPairs: list[ClipPromptPair] = Field(min_length=1, max_length=4)
    images: list[ClipVisualImage] = Field(min_length=1, max_length=100_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "ClipVisualEvaluationRequest":
        image_ids = [image.imageId for image in self.images]
        hashes = [image.imageSha256 for image in self.images]
        prompt_codes = [prompt.attributeCode for prompt in self.promptPairs]
        if (
            len(image_ids) != len(set(image_ids))
            or len(hashes) != len(set(hashes))
            or self.requestedAttributeCodes != prompt_codes
            or len(prompt_codes) != len(set(prompt_codes))
            or any(image.capturedAt > self.occurredAt for image in self.images)
        ):
            raise ValueError("CLIP_DATASET_INVALID")
        return self


class ClipAttributeMetric(StrictContract):
    """Métrica por atributo con matriz de confusión explícita."""

    attributeCode: Version
    truePositive: int = Field(ge=0)
    falsePositive: int = Field(ge=0)
    falseNegative: int = Field(ge=0)
    precision: float = Field(ge=0, le=1)
    recall: float = Field(ge=0, le=1)


class ClipAuxiliaryEvidence(StrictContract):
    """Candidato de evidencia; requiere revisión y nunca muta el perfil automáticamente."""

    imageId: UUID
    imageSha256: str
    venueId: UUID
    attributeCode: Version
    confidence: float = Field(ge=0, le=1)
    source: Literal["imageAuxiliary"] = "imageAuxiliary"
    humanReviewRequired: Literal[True] = True


class ClipVisualEvaluationResponse(StrictContract):
    """Informe de calidad y evidencia auxiliar minimizada con puertas fail-closed."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    policyVersion: Version
    modelKey: Version
    modelRevision: str
    datasetVersion: Version
    evaluatedImageCount: int = Field(ge=0)
    suppressedPeopleImageCount: int = Field(ge=0)
    metrics: list[ClipAttributeMetric]
    macroPrecision: float = Field(ge=0, le=1)
    macroRecall: float = Field(ge=0, le=1)
    qualityGatesPassed: bool
    productionEvidence: bool
    auxiliaryEvidenceReviewAllowed: bool
    evidences: list[ClipAuxiliaryEvidence]
    prohibitedClaimsEmitted: Literal[0]
    automaticProfileMutationAllowed: Literal[False]


class ClipVisualEvaluator:
    """Compara embeddings con pares de prompts y evalúa contra etiquetas humanas."""

    def __init__(self, policy: ClipVisualPolicy, manifest: ClipVisualManifest) -> None:
        self.policy = policy
        self.manifest = manifest
        if policy.modelKey != manifest.modelKey:
            raise ValueError("CLIP_POLICY_MODEL_MISMATCH")

    def evaluate(self, request: ClipVisualEvaluationRequest) -> ClipVisualEvaluationResponse:
        """Suprime imágenes con personas y solo emite códigos visuales allowlist."""
        if request.policyVersion != self.policy.policyVersion:
            raise ValueError("CLIP_POLICY_VERSION_MISMATCH")
        if not set(request.requestedAttributeCodes) <= set(self.manifest.allowedAttributeCodes):
            raise ValueError("CLIP_ATTRIBUTE_NOT_ALLOWED")
        self._validate_versions(request)
        usable = [image for image in request.images if not image.peopleDetected]
        if any(
            not set(image.humanVerifiedAttributes) <= set(request.requestedAttributeCodes)
            for image in usable
        ):
            raise ValueError("CLIP_HUMAN_LABEL_NOT_ALLOWED")
        predictions: dict[UUID, dict[str, float]] = {}
        prompts = {prompt.attributeCode: prompt for prompt in request.promptPairs}
        for image in usable:
            scores = {
                code: self._confidence(image.embedding, prompts[code])
                for code in request.requestedAttributeCodes
            }
            predictions[image.imageId] = scores
        metrics = self._metrics(usable, predictions, request.requestedAttributeCodes)
        macro_precision = sum(metric.precision for metric in metrics) / len(metrics)
        macro_recall = sum(metric.recall for metric in metrics) / len(metrics)
        gates = (
            len(usable) >= self.policy.minimumImages
            and macro_precision >= self.policy.minimumMacroPrecision
            and macro_recall >= self.policy.minimumMacroRecall
        )
        review_allowed = gates and request.productionEvidence
        evidences = self._evidences(usable, predictions) if review_allowed else []
        return ClipVisualEvaluationResponse(
            requestId=request.requestId,
            policyVersion=self.policy.policyVersion,
            modelKey=self.manifest.modelKey,
            modelRevision=self.manifest.revision,
            datasetVersion=request.datasetVersion,
            evaluatedImageCount=len(usable),
            suppressedPeopleImageCount=len(request.images) - len(usable),
            metrics=metrics,
            macroPrecision=round(macro_precision, 8),
            macroRecall=round(macro_recall, 8),
            qualityGatesPassed=gates,
            productionEvidence=request.productionEvidence,
            auxiliaryEvidenceReviewAllowed=review_allowed,
            evidences=evidences,
            prohibitedClaimsEmitted=0,
            automaticProfileMutationAllowed=False,
        )

    def _validate_versions(self, request: ClipVisualEvaluationRequest) -> None:
        vectors = [
            vector
            for pair in request.promptPairs
            for vector in (pair.positive, pair.negative)
        ] + [image.embedding for image in request.images]
        if any(
            vector.modelKey != self.manifest.modelKey
            or vector.modelRevision != self.manifest.revision
            for vector in vectors
        ):
            raise ValueError("CLIP_VECTOR_VERSION_MISMATCH")

    def _confidence(self, image: NormalizedClipVector, prompt: ClipPromptPair) -> float:
        positive = sum(a * b for a, b in zip(image.values, prompt.positive.values, strict=True))
        negative = sum(a * b for a, b in zip(image.values, prompt.negative.values, strict=True))
        delta = max(-60.0, min(60.0, (positive - negative) * self.policy.similarityLogitScale))
        return round(1.0 / (1.0 + math.exp(-delta)), 8)

    def _metrics(
        self,
        images: list[ClipVisualImage],
        predictions: dict[UUID, dict[str, float]],
        codes: list[str],
    ) -> list[ClipAttributeMetric]:
        metrics: list[ClipAttributeMetric] = []
        for code in codes:
            counts: Counter[str] = Counter()
            for image in images:
                predicted = predictions[image.imageId][code] >= self.policy.predictionThreshold
                actual = code in image.humanVerifiedAttributes
                counts["tp" if predicted and actual else "fp" if predicted else "fn" if actual else "tn"] += 1
            precision = counts["tp"] / (counts["tp"] + counts["fp"]) if counts["tp"] + counts["fp"] else 0.0
            recall = counts["tp"] / (counts["tp"] + counts["fn"]) if counts["tp"] + counts["fn"] else 0.0
            metrics.append(
                ClipAttributeMetric(
                    attributeCode=code,
                    truePositive=counts["tp"],
                    falsePositive=counts["fp"],
                    falseNegative=counts["fn"],
                    precision=round(precision, 8),
                    recall=round(recall, 8),
                )
            )
        return metrics

    def _evidences(
        self,
        images: list[ClipVisualImage],
        predictions: dict[UUID, dict[str, float]],
    ) -> list[ClipAuxiliaryEvidence]:
        evidences: list[ClipAuxiliaryEvidence] = []
        for image in images:
            selected = sorted(
                (
                    (code, confidence)
                    for code, confidence in predictions[image.imageId].items()
                    if confidence >= self.policy.predictionThreshold
                ),
                key=lambda item: (-item[1], item[0]),
            )[: self.policy.maximumEvidencePerImage]
            evidences.extend(
                ClipAuxiliaryEvidence(
                    imageId=image.imageId,
                    imageSha256=image.imageSha256,
                    venueId=image.venueId,
                    attributeCode=code,
                    confidence=confidence,
                )
                for code, confidence in selected
            )
        return evidences
