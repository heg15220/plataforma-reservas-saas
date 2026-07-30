package com.reserly.platform.administration.dto;

import java.time.Instant;
import java.util.UUID;

/** Restricción administrativa sin datos de reserva adicionales. */
public record AdminPenaltyResponse(
    UUID id,
    String customerEmailNormalized,
    String scope,
    UUID venueId,
    int incidentCountOperational,
    Instant startsAt,
    Instant endsAt,
    String status,
    String reason,
    UUID createdFromIncidentId,
    Instant updatedAt) {}
