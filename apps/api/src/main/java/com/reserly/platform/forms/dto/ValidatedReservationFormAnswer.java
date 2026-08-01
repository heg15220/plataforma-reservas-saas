package com.reserly.platform.forms.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/** Respuesta normalizada con snapshots suficientes para la persistencia histórica de fase 7. */
public record ValidatedReservationFormAnswer(
    UUID fieldId, String fieldKey, String fieldLabel, String type, JsonNode value) {

  public ValidatedReservationFormAnswer {
    value = value.deepCopy();
  }
}
