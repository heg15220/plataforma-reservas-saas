package com.reserly.platform.reservations.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Snapshot post-commit del aviso de cancelación dirigido al cliente.
 *
 * <p>El motivo viaja solo por la cola privada y se renderiza escapado; no debe escribirse en logs.
 */
public record VenueReservationCancellationEmailRequestedEvent(
    UUID eventId,
    UUID reservationId,
    String customerEmail,
    String customerLocale,
    String venueName,
    String venueAddress,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize,
    String cancellationReason) {}
