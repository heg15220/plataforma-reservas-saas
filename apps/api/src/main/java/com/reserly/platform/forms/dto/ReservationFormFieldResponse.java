package com.reserly.platform.forms.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proyecci?n privada localizada de un campo, sin exponer identidad del propietario. */
public record ReservationFormFieldResponse(
    UUID id,
    String label,
    ReservationFormLocalizedTextDto labelI18n,
    String key,
    String type,
    boolean required,
    List<String> options,
    List<ReservationFormLocalizedTextDto> optionsI18n,
    int position,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
