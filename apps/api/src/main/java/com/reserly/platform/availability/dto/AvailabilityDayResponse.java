package com.reserly.platform.availability.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Estado efectivo configurado para una fecha concreta del local. */
public record AvailabilityDayResponse(
    LocalDate date,
    boolean closed,
    boolean reservationsEnabled,
    String source,
    UUID blockId,
    String reason) {}
