package com.reserly.platform.venues.dto;

import java.time.Instant;
import java.util.UUID;

/** Asociación privada entre un local publicado propio y su destinatario operativo. */
public record VenueEmailAssignmentResponse(
    UUID venueId,
    String venueName,
    String venueSlug,
    String email,
    boolean panelAccessConfigured,
    Instant updatedAt) {}
