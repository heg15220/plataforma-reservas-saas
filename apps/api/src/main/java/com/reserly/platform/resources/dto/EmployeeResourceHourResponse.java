package com.reserly.platform.resources.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/** Proyeccion privada de un dia de horario semanal de recurso. */
public record EmployeeResourceHourResponse(
    UUID id,
    int weekday,
    boolean available,
    LocalTime startsAt,
    LocalTime endsAt,
    Instant createdAt,
    Instant updatedAt) {}
