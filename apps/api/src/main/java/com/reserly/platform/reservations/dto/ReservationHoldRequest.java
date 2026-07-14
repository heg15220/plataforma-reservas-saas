package com.reserly.platform.reservations.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/** Selección pública mínima para reservar temporalmente una franja. */
public record ReservationHoldRequest(
    @NotNull UUID venueId,
    @NotNull UUID timeSlotId,
    UUID serviceId,
    UUID employeeResourceId,
    @Pattern(regexp = "^(any_available|specific)$") String assignmentPreference,
    @Min(1) int partySize) {}
