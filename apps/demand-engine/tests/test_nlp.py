"""Pruebas del pipeline NLP ES/EN, negación, multilabel y límites de privacidad."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.nlp import (
    NlpAnalyzeRequest,
    PersonalCareNlpPipeline,
    PersonalCareNlpPolicy,
)


POLICY = Path(__file__).parents[1] / "policies" / "nlp-personal-care.v1.json"


class PersonalCareNlpTests(unittest.TestCase):
    """Demuestra normalización bilingüe y que solo salen conceptos canónicos permitidos."""

    def setUp(self) -> None:
        self.pipeline = PersonalCareNlpPipeline(PersonalCareNlpPolicy.load(POLICY))

    def _request(
        self, text: str, locale: str = "es", policy: str = "nlp-personal-care-v1"
    ) -> NlpAnalyzeRequest:
        return NlpAnalyzeRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": datetime(2026, 8, 20, 12, tzinfo=UTC),
                "locale": locale,
                "policyVersion": policy,
                "purpose": "personalCareSearch",
                "text": text,
            }
        )

    def test_spanish_normalization_synonyms_and_multilabel(self) -> None:
        result = self.pipeline.analyze(
            self._request("Quiero CORTARME el pelo hoy, en un sitio con luz natural")
        )
        self.assertEqual(
            ["haircut", "sameDayAvailability", "naturalLight"],
            [item.conceptCode for item in result.entities],
        )
        self.assertEqual(
            ["ambiencePreference", "availabilityIntent", "serviceIntent"],
            [item.labelCode for item in result.labels],
        )
        self.assertEqual("es", result.language)

    def test_english_longest_phrase_and_multilabel(self) -> None:
        result = self.pipeline.analyze(
            self._request("Hair cut with a late appointment in a quiet salon", "en")
        )
        self.assertEqual(
            ["haircut", "eveningAvailability", "lowNoiseAppointments"],
            [item.conceptCode for item in result.entities],
        )
        self.assertEqual(3, len(result.labels))

    def test_negation_keeps_entity_but_does_not_activate_label(self) -> None:
        result = self.pipeline.analyze(self._request("No quiero manicura, quiero maquillaje"))
        by_code = {item.conceptCode: item for item in result.entities}
        self.assertEqual("negative", by_code["nailService"].polarity)
        self.assertEqual("positive", by_code["makeupService"].polarity)
        service_label = next(item for item in result.labels if item.labelCode == "serviceIntent")
        self.assertEqual(["makeupService"], service_label.evidenceConceptCodes)

    def test_rejects_email_phone_sensitive_terms_and_policy_drift(self) -> None:
        rejected = [
            "corte para ana@example.com",
            "manicura, teléfono +34 612 345 678",
            "tratamiento para mi embarazo",
            "corte relacionado con medication",
            "!!!",
        ]
        for text in rejected:
            with self.subTest(text=text), self.assertRaises(ValueError):
                self.pipeline.analyze(self._request(text))
        with self.assertRaises(ValueError):
            self.pipeline.analyze(self._request("corte", policy="unknown-v1"))

    def test_replay_is_deterministic_and_response_contains_no_text(self) -> None:
        request = self._request("Busco una limpieza facial tranquila")
        first = self.pipeline.analyze(request)
        second = self.pipeline.analyze(request)
        self.assertEqual(first, second)
        serialized = first.model_dump_json()
        self.assertNotIn("limpieza", serialized)
        self.assertNotIn("text", serialized.casefold())


if __name__ == "__main__":
    unittest.main()
