package com.reserly.platform.reservations.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Detalle privado básico de una reserva del local.
 *
 * <p>Las respuestas personalizadas y la presentación del recurso asignado se incorporan en las
 * tareas 9.4 y 9.5. Los hashes y secretos de gestión nunca forman parte del contrato.
 */
public record VenueReservationDetailResponse(
    UUID id,
    UUID timeSlotId,
    UUID serviceId,
    String customerName,
    String customerEmail,
    int partySize,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    String status,
    Instant cancelledAt,
    String cancelledBy,
    String cancellationReason,
    Instant createdAt,
    Instant updatedAt) {}
