package com.reserly.platform.availability.dto;

import java.time.LocalTime;
import java.util.UUID;

/** Día de horario semanal devuelto al panel privado. */
public record OpeningHourResponse(
    UUID id,
    int weekday,
    boolean closed,
    boolean reservationsEnabled,
    LocalTime opensAt,
    LocalTime closesAt) {}
