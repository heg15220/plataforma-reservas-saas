package com.reserly.platform.availability.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Payload privado para crear una franja manual sin exponer el ID del local. */
public record TimeSlotRequest(
    @NotNull LocalDate date,
    @NotNull LocalTime startsAt,
    @NotNull LocalTime endsAt,
    @Min(1) int capacity,
    UUID serviceId) {}
