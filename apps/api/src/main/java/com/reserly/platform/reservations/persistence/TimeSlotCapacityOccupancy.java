package com.reserly.platform.reservations.persistence;

import java.util.UUID;

/**
 * Ocupación agregada de una franja pública.
 *
 * @param timeSlotId franja a la que pertenecen las reservas activas
 * @param occupiedCapacity suma de personas confirmadas o retenidas mediante un hold vigente
 */
public record TimeSlotCapacityOccupancy(UUID timeSlotId, Long occupiedCapacity) {}
