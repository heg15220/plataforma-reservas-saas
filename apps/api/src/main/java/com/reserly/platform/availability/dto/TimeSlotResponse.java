package com.reserly.platform.availability.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Franja de reserva visible en el panel privado del local. */
public record TimeSlotResponse(
    UUID id,
    LocalDate date,
    int weekday,
    LocalTime startsAt,
    LocalTime endsAt,
    int capacity,
    String status,
    boolean createdByRule,
    long version) {}
