package com.reserly.platform.notifications;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/** Datos mínimos para la confirmación dirigida al usuario; nunca incluye credenciales del local. */
public record ReservationConfirmationTemplateData(
    String venueName,
    String venueAddress,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize,
    String bookingRules,
    URI manageUrl,
    Instant manageTokenExpiresAt,
    List<Answer> answers) {

  public ReservationConfirmationTemplateData {
    Objects.requireNonNull(venueName);
    Objects.requireNonNull(venueAddress);
    Objects.requireNonNull(date);
    Objects.requireNonNull(startsAt);
    Objects.requireNonNull(endsAt);
    Objects.requireNonNull(bookingRules);
    Objects.requireNonNull(manageUrl);
    Objects.requireNonNull(manageTokenExpiresAt);
    if (partySize < 1) {
      throw new IllegalArgumentException("partySize debe ser positivo");
    }
    answers = answers == null ? List.of() : List.copyOf(answers);
  }

  /** Par etiqueta/valor autorizado para mostrarse al titular de la reserva. */
  public record Answer(String label, String value) {
    public Answer {
      Objects.requireNonNull(label);
      Objects.requireNonNull(value);
    }
  }
}
