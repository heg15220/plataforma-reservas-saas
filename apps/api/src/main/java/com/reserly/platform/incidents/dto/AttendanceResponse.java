package com.reserly.platform.incidents.dto;

import java.time.Instant;
import java.util.UUID;

/** Resultado minimizado del marcado de asistencia de una reserva propia. */
public record AttendanceResponse(
    UUID reservationId, String status, Instant attendanceMarkedAt, Instant updatedAt) {}
