from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image

from reserly_demand_engine.full_taxonomy_visual_hybrid_v5 import (
    CLASSIC_DIMENSIONS,
    classic_pixel_features,
    predict_scores,
)


ROOT = Path(__file__).resolve().parents[3]
REPORT = ROOT / "apps/demand-engine/evaluation/results/full-taxonomy-visual-hybrid-development.v5.json"
MODEL = ROOT / "apps/demand-engine/models/full-taxonomy-visual-hybrid-classifier.v5.json"
CLASSIC = ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3/development-classic-pixel-features.v5.npz"


def test_classic_features_are_deterministic_and_finite(tmp_path: Path) -> None:
    image = tmp_path / "sample.png"
    pixels = np.zeros((96, 128, 3), dtype=np.uint8)
    pixels[:, :64, 0] = 255
    pixels[:, 64:, 1] = 180
    Image.fromarray(pixels).save(image)
    first = classic_pixel_features(image)
    second = classic_pixel_features(image)
    assert first.shape == (CLASSIC_DIMENSIONS,)
    assert np.isfinite(first).all()
    assert np.array_equal(first, second)


def test_v5_improves_v4_without_claiming_independent_test() -> None:
    report = json.loads(REPORT.read_text(encoding="utf-8"))
    assert report["imageCount"] == 1016
    assert report["newImagesGenerated"] == 0
    assert report["meanAccuracyUplift"] > 0
    assert report["meanMacroF1Uplift"] > 0
    assert report["worstFoldAccuracyUplift"] > 0
    assert report["worstFoldMacroF1Uplift"] > 0
    assert report["independentTestAvailable"] is False
    assert report["qualityConfirmed"] is False
    assert report["promotionAllowed"] is False


def test_v5_artifacts_and_inference_contract() -> None:
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    assert model["inputFeatures"] == [
        "clipGlobalEmbedding512", "clipCenter80Embedding512", "classicPixelFeatures336"
    ]
    assert {"prompt", "typeCode", "familyCode", "archetypeCode"}.issubset(
        model["prohibitedInputFeatures"]
    )
    with np.load(CLASSIC, allow_pickle=False) as artifact:
        assert artifact["features"].shape == (1016, CLASSIC_DIMENSIONS)
    global_x = np.asarray(model["globalPrototypes"][:2], dtype=np.float64)
    center_x = np.asarray(model["centerPrototypes"][:2], dtype=np.float64)
    classic = np.tile(np.asarray(model["classicMean"]), (2, 1))
    scores = predict_scores(model, global_x, center_x, classic)
    assert scores.shape == (2, 23)
    assert np.isfinite(scores).all()
