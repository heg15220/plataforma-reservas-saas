"""Pruebas ejecutables de gobierno, jerarquía, bilingüismo y prohibiciones."""

import json
import unittest
from pathlib import Path

from pydantic import ValidationError

from reserly_demand_contracts.ontology_v1 import DemandOntologyV1


ROOT = Path(__file__).parents[1]


class DemandOntologyV1Tests(unittest.TestCase):
    """Audita el documento que 19.13 convertirá en seed gobernado."""

    def load_payload(self) -> dict[str, object]:
        """Carga una copia mutable para casos negativos."""
        return json.loads(
            (ROOT / "ontology/personal-care.v1.json").read_text(encoding="utf-8")
        )

    def validate_payload(self, payload: dict[str, object]) -> DemandOntologyV1:
        """Valida en modo JSON estricto, igual que lo consumirá la futura ingesta."""
        return DemandOntologyV1.model_validate_json(json.dumps(payload))

    def test_catalog_has_44_governed_bilingual_attributes(self) -> None:
        """Fija vertical, tamaño, familias, traducciones y fuentes iniciales."""
        ontology = self.validate_payload(self.load_payload())
        self.assertEqual(len(ontology.attributes), 44)
        self.assertEqual(len(ontology.families), 6)
        self.assertEqual(len(ontology.sources), 6)
        self.assertTrue(all(item.name.es and item.name.en for item in ontology.attributes))
        self.assertTrue(all(item.definition.es and item.definition.en for item in ontology.attributes))
        schema = json.loads(
            (ROOT / "schemas/demand-ontology.v1.schema.json").read_text(encoding="utf-8")
        )
        self.assertFalse(schema["additionalProperties"])
        self.assertEqual(schema["properties"]["attributes"]["minItems"], 30)
        self.assertEqual(schema["properties"]["attributes"]["maxItems"], 50)

    def test_hierarchy_sources_validity_and_subjective_evidence_are_consistent(self) -> None:
        """Ejecuta restricciones que no deben depender del futuro panel admin."""
        ontology = self.validate_payload(self.load_payload())
        by_code = {attribute.code: attribute for attribute in ontology.attributes}
        self.assertEqual(by_code["hairCutService"].parentCode, "hairServices")
        self.assertEqual(by_code["sameDayAvailability"].validity.ttlDays, 1)
        self.assertGreaterEqual(by_code["appointmentPunctuality"].minimumEvidence, 5)

    def test_prohibited_attributes_are_explicit_and_never_overlap_public_catalog(self) -> None:
        """Impide normalizar como feature una inferencia sensible o no verificable."""
        ontology = self.validate_payload(self.load_payload())
        published = {attribute.code for attribute in ontology.attributes}
        prohibited = {attribute.code for attribute in ontology.prohibitedAttributes}
        self.assertGreaterEqual(len(prohibited), 20)
        self.assertFalse(published & prohibited)
        self.assertIn("medicalCondition", prohibited)
        self.assertIn("hygieneInference", prohibited)
        self.assertIn("psychologicalProfile", prohibited)

    def test_rejects_unknown_source_cycle_and_invalid_dynamic_validity(self) -> None:
        """Comprueba fallos cerrados ante tres corrupciones de gobierno."""
        unknown_source = self.load_payload()
        unknown_source["attributes"][0]["allowedSources"] = ["dataBroker"]
        with self.assertRaises(ValidationError):
            self.validate_payload(unknown_source)

        cycle = self.load_payload()
        cycle["attributes"][0]["parentCode"] = cycle["attributes"][0]["code"]
        with self.assertRaises(ValidationError):
            self.validate_payload(cycle)

        no_ttl = self.load_payload()
        dynamic = next(item for item in no_ttl["attributes"] if item["type"] == "dynamic")
        dynamic["validity"] = {"mode": "untilRetired", "ttlDays": None}
        with self.assertRaises(ValidationError):
            self.validate_payload(no_ttl)


if __name__ == "__main__":
    unittest.main()
