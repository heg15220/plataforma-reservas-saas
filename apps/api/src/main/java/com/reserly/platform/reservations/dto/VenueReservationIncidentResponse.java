package com.reserly.platform.reservations.dto;

import java.time.Instant;

/**
 * Señal mínima de una incidencia profesional.
 *
 * <p>No expone local, reserva, actor, email ni notas del reporte.
 */
public record VenueReservationIncidentResponse(
    String incidentType, Instant reportedAt, String status) {}
