package com.reserly.platform.reservations.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Snapshot público de la reserva inmediatamente después de confirmarla. */
public record ReservationConfirmResponse(
    String status,
    UUID reservationId,
    String manageUrlSentTo,
    String venueName,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize) {}
