package com.reserly.platform.availability.dto;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Franja pública consultable por usuarios finales.
 *
 * <p>La capacidad disponible coincide temporalmente con la capacidad total hasta que Fase 7
 * incorpore reservas confirmadas y holds vigentes.
 */
public record PublicTimeSlotAvailabilityResponse(
    UUID slotId,
    LocalTime startsAt,
    LocalTime endsAt,
    int capacity,
    int availableCapacity,
    String status,
    boolean bookingAvailable) {}
