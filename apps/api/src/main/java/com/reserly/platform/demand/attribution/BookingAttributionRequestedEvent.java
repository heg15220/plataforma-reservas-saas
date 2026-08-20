package com.reserly.platform.demand.attribution;

import java.time.Instant;
import java.util.UUID;

/** Solicita atribución tras una confirmación ya comprometida, sin transportar PII. */
public record BookingAttributionRequestedEvent(
    UUID reservationId, UUID requestId, Instant confirmedAt) {}
