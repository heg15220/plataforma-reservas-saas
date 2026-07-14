package com.reserly.platform.reservations.dto;

import java.time.Instant;
import java.util.UUID;

/** Secreto de proceso devuelto una sola vez al cliente que obtuvo el hold. */
public record ReservationHoldResponse(
    UUID reservationId, String holdToken, Instant expiresAt, long remainingSeconds) {}
