package com.reserly.platform.reservations.dto;

import java.time.Instant;

/** Confirmación mínima; el token queda revocado y no se devuelve. */
public record ReservationCancellationResponse(String status, Instant cancelledAt) {}
