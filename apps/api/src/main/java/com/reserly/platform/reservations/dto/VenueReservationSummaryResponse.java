package com.reserly.platform.reservations.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Resumen privado de una reserva recibida por el local.
 *
 * @param id identificador de la reserva
 * @param timeSlotId franja histórica asociada
 * @param customerName nombre confirmado del cliente
 * @param customerEmail email confirmado del cliente
 * @param partySize plazas reservadas
 * @param date fecha local de la cita
 * @param startsAt inicio local
 * @param endsAt fin local
 * @param status estado visible: pendiente antes del inicio y confirmado desde la hora reservada
 * @param manualActionsAvailable si el actor puede decidir asistencia o cancelar ahora
 * @param incidentRiskLevel resumen informativo low, watch o high sin detalles del historial
 * @param createdAt instante de creación
 */
public record VenueReservationSummaryResponse(
    UUID id,
    UUID timeSlotId,
    String customerName,
    String customerEmail,
    int partySize,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    String status,
    boolean manualActionsAvailable,
    String incidentRiskLevel,
    Instant createdAt) {}
