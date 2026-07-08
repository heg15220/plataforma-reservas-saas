package com.reserly.platform.availability.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Configuración excepcional de una fecha concreta del local autenticado. */
public record AvailabilityDayRequest(
    @NotNull LocalDate date,
    boolean closed,
    boolean reservationsEnabled,
    @Size(max = 500) String reason) {}
