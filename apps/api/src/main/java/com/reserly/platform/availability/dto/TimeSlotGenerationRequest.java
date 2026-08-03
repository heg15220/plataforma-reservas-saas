package com.reserly.platform.availability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** Payload privado para generar franjas automáticas de una fecha por duración fija. */
public record TimeSlotGenerationRequest(
    @NotNull LocalDate date,
    @Min(5) @Max(480) int durationMinutes,
    @Min(1) int capacity,
    UUID serviceId) {}
