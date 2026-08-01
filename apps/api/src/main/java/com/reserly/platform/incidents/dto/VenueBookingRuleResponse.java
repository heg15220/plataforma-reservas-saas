package com.reserly.platform.incidents.dto;

import java.time.Instant;

/** Proyección privada y minimizada de las reglas básicas de cancelación. */
public record VenueBookingRuleResponse(
    boolean cancellationAllowed, int freeCancellationUntilMinutesBefore, Instant updatedAt) {}
