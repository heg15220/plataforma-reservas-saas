package com.reserly.platform.notifications;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Datos de la nueva reserva que el local necesita para operarla.
 *
 * <p>No contiene el token de gestión del usuario ni enlaces públicos. Las respuestas se copian para
 * impedir mutaciones durante el renderizado asíncrono.
 *
 * <p>{@code panelUrl} solo apunta al panel autenticado; nunca incorpora el token público del
 * usuario.
 */
public record VenueReservationNotificationTemplateData(
    String venueName,
    String customerName,
    String customerEmail,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize,
    List<Answer> answers,
    URI panelUrl) {

  public VenueReservationNotificationTemplateData {
    Objects.requireNonNull(venueName);
    Objects.requireNonNull(customerName);
    Objects.requireNonNull(customerEmail);
    Objects.requireNonNull(date);
    Objects.requireNonNull(startsAt);
    Objects.requireNonNull(endsAt);
    Objects.requireNonNull(panelUrl);
    if (partySize < 1) {
      throw new IllegalArgumentException("partySize debe ser positivo");
    }
    answers = answers == null ? List.of() : List.copyOf(answers);
  }

  /** Respuesta confirmada que el propietario está autorizado a consultar. */
  public record Answer(String label, String value) {
    public Answer {
      Objects.requireNonNull(label);
      Objects.requireNonNull(value);
    }
  }
}
