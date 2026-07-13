package com.reserly.platform.forms.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proyeccion privada de un campo personalizado, sin exponer identidad del propietario. */
public record ReservationFormFieldResponse(
    UUID id,
    String label,
    String key,
    String type,
    boolean required,
    List<String> options,
    int position,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
