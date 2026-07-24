package com.reserly.platform.reservations.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Proyección mínima visible mediante posesión del secreto de gestión. */
public record ManagedReservationResponse(
    UUID reservationId,
    String venueName,
    String venueAddress,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    int partySize,
    String status,
    boolean cancellable,
    Instant cancellationDeadline,
    int cancellationNoticeMinutes) {}