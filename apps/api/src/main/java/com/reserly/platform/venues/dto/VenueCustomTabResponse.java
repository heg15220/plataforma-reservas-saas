package com.reserly.platform.venues.dto;

import java.time.Instant;
import java.util.UUID;

/** Proyección privada de una pestaña sin exponer local, propietario ni constraints físicos. */
public record VenueCustomTabResponse(
    UUID id,
    VenueCustomTabLocalizedTextDto titleI18n,
    VenueCustomTabLocalizedTextDto contentI18n,
    int position,
    boolean active,
    String contentFormat,
    Instant createdAt,
    Instant updatedAt) {}
