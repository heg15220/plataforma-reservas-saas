package com.reserly.platform.availability.dto;

import java.time.LocalDate;
import java.util.List;

/** Respuesta pública de disponibilidad de un local publicado para una fecha concreta. */
public record PublicVenueAvailabilityResponse(
    String venueSlug,
    LocalDate date,
    int weekday,
    String statusCode,
    String statusLabel,
    boolean bookingAvailable,
    boolean closed,
    boolean reservationsEnabled,
    String source,
    int availableSlotCount,
    List<PublicTimeSlotAvailabilityResponse> slots) {}
