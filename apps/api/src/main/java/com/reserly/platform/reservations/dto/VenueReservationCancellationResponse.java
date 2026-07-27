package com.reserly.platform.reservations.dto;

import java.time.Instant;
import java.util.UUID;

/** Resultado mínimo de la cancelación preventiva de una reserva propia. */
public record VenueReservationCancellationResponse(
    UUID reservationId, String status, Instant cancelledAt) {}
