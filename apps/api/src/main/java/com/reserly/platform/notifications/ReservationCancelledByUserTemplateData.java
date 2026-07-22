package com.reserly.platform.notifications;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Snapshot del aviso dirigido al local cuando el titular cancela.
 *
 * <p>No incluye token de gestión ni motivo libre porque la cancelación pública no lo exige.
 */
public record ReservationCancelledByUserTemplateData(
    String venueName,
    String customerName,
    String customerEmail,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize) {

  public ReservationCancelledByUserTemplateData {
    Objects.requireNonNull(venueName);
    Objects.requireNonNull(customerName);
    Objects.requireNonNull(customerEmail);
    Objects.requireNonNull(date);
    Objects.requireNonNull(startsAt);
    Objects.requireNonNull(endsAt);
    if (partySize < 1) {
      throw new IllegalArgumentException("partySize debe ser positivo");
    }
  }
}
