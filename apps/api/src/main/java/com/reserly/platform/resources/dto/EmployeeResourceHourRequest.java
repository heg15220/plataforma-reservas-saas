package com.reserly.platform.resources.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

/** Dia de horario semanal basico de un recurso privado del local. */
public record EmployeeResourceHourRequest(
    @Min(1) @Max(7) int weekday, boolean available, LocalTime startsAt, LocalTime endsAt) {}
