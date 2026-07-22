package com.reserly.platform.notifications;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Snapshot del aviso dirigido al usuario cuando el local cancela por una causa operativa.
 *
 * <p>El motivo procede del registro auditado futuro y debe renderizarse siempre escapado.
 */
public record ReservationCancelledByVenueTemplateData(
    String venueName,
    String venueAddress,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize,
    String cancellationReason) {

  public ReservationCancelledByVenueTemplateData {
    Objects.requireNonNull(venueName);
    Objects.requireNonNull(venueAddress);
    Objects.requireNonNull(date);
    Objects.requireNonNull(startsAt);
    Objects.requireNonNull(endsAt);
    Objects.requireNonNull(cancellationReason);
    if (partySize < 1) {
      throw new IllegalArgumentException("partySize debe ser positivo");
    }
    if (cancellationReason.isBlank()) {
      throw new IllegalArgumentException("cancellationReason es obligatorio");
    }
  }
}
