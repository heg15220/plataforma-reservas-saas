package com.reserly.platform.reservations.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Snapshot de una respuesta personalizada tal como fue confirmada.
 *
 * @param fieldKey clave histórica del campo
 * @param fieldLabel etiqueta histórica mostrada al cliente
 * @param value valor JSON validado y normalizado
 * @param createdAt instante de persistencia
 */
public record VenueReservationFormAnswerResponse(
    String fieldKey, String fieldLabel, JsonNode value, Instant createdAt) {

  public VenueReservationFormAnswerResponse {
    value = value == null ? null : value.deepCopy();
  }
}
