package com.reserly.platform.forms.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/** Respuesta por ID recibida desde un caso de uso externo al módulo de formularios. */
public record ReservationFormFieldAnswer(UUID fieldId, JsonNode value) {

  public ReservationFormFieldAnswer {
    value = value == null ? null : value.deepCopy();
  }
}
