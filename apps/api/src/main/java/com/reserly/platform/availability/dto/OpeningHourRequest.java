package com.reserly.platform.availability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

/** Día de horario semanal recibido desde el panel privado del local. */
public record OpeningHourRequest(
    @Min(1) @Max(7) int weekday,
    boolean closed,
    boolean reservationsEnabled,
    LocalTime opensAt,
    LocalTime closesAt) {}
