"""Pruebas del catálogo candidato de 254 tipos físicos."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

from reserly_demand_contracts.venue_taxonomy_v1 import VenueTaxonomyV1


ROOT = Path(__file__).parents[1]


class VenueTaxonomyV1Tests(unittest.TestCase):
    """Audita cobertura, minimización de metadatos y compatibilidad histórica."""

    def payload(self) -> dict[str, object]:
        return json.loads((ROOT / "catalog/venue-taxonomy.v1.json").read_text(encoding="utf-8"))

    def validate(self, payload: dict[str, object]) -> VenueTaxonomyV1:
        return VenueTaxonomyV1.model_validate_json(json.dumps(payload, ensure_ascii=False))

    def test_catalog_has_complete_candidate_coverage(self) -> None:
        catalog = self.validate(self.payload())
        self.assertEqual(23, len(catalog.families))
        self.assertEqual(254, len(catalog.types))
        self.assertEqual(8, len(catalog.legacyCompatibility))
        self.assertEqual(list(range(1, 255)), sorted(item.sourceId for item in catalog.types))
        self.assertTrue(all(item.name.es for item in catalog.types))
        self.assertTrue(all(item.name.en is None for item in catalog.types))
        self.assertEqual("candidateOnly", catalog.activationStatus)

    def test_types_keep_only_product_taxonomy_fields(self) -> None:
        catalog = self.validate(self.payload())
        self.assertEqual(
            {"fileSha256", "sourceVersion", "recordCount", "scopeEs"},
            set(catalog.source.model_dump()),
        )
        self.assertEqual(
            {
                "sourceId", "code", "familyCode", "subcategoryCode", "name",
                "sourceSubcategoryEs", "useCode", "useLabelEs", "translationStatus",
                "governanceStatus",
            },
            set(catalog.types[0].model_dump()),
        )

    def test_municipal_is_operator_and_other_requires_reclassification(self) -> None:
        catalog = self.validate(self.payload())
        mappings = {item.legacyCategoryCode: item for item in catalog.legacyCompatibility}
        self.assertEqual("operatorAttribute", mappings["instalacion-municipal"].mappingKind)
        self.assertEqual("public-municipal", mappings["instalacion-municipal"].operatorTypeCode)
        self.assertEqual([], mappings["instalacion-municipal"].targetTypeCodes)
        self.assertEqual("compositeRequiresReview", mappings["otros"].mappingKind)
        self.assertFalse(any(item.existingImagesEligibleAsNewTest for item in mappings.values()))


if __name__ == "__main__":
    unittest.main()
