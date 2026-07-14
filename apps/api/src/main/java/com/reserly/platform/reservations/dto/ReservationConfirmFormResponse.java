package com.reserly.platform.reservations.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Respuesta pública sin validar de un campo personalizado; su consumo corresponde a 7.9. */
public record ReservationConfirmFormResponse(
    @NotNull UUID fieldId,
    @NotNull JsonNode value) {}
