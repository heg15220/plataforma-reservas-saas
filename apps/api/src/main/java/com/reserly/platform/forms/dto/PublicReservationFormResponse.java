package com.reserly.platform.forms.dto;

import java.util.List;
import java.util.UUID;

/** Esquema anónimo renderizable para iniciar una reserva en un local publicado. */
public record PublicReservationFormResponse(
    UUID venueId, String venueSlug, List<ReservationFormPreviewFieldResponse> fields) {
  public PublicReservationFormResponse { fields = List.copyOf(fields); }
}
