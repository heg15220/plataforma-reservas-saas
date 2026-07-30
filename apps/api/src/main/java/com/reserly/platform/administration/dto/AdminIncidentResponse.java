package com.reserly.platform.administration.dto;

import java.time.Instant;
import java.util.UUID;

/** Evidencia operativa completa visible solo bajo autorización administrativa. */
public record AdminIncidentResponse(
    UUID id, UUID reservationId, UUID venueId, String venueName,
    String customerEmailNormalized, String incidentType, UUID reportedByUserId,
    Instant reportedAt, String notes, String status) {}
