package com.reserly.platform.reservations.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Trabajo inmutable para entregar confirmación al cliente y aviso al local tras el commit.
 *
 * <p>{@code manageToken} es sensible: puede serializarse en el transporte de entrega, pero nunca
 * registrarse ni persistirse en claro en PostgreSQL.
 */
public record ReservationConfirmationEmailRequestedEvent(
    UUID eventId,
    UUID reservationId,
    String customerName,
    String customerEmail,
    String venueName,
    String venueEmail,
    String venueAddress,
    String locale,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize,
    String bookingRules,
    String manageToken,
    Instant manageTokenExpiresAt,
    List<ReservationConfirmationEmailAnswer> formResponses) {

  public ReservationConfirmationEmailRequestedEvent {
    formResponses = formResponses == null ? List.of() : List.copyOf(formResponses);
  }
}
