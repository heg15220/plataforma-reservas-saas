package com.reserly.platform.forms.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** Respuesta recibida para una clave concreta antes de aplicar validación de esquema. */
public record ReservationFormAnswerCommand(String key, JsonNode value) {}
