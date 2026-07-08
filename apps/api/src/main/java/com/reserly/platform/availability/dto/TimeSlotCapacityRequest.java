package com.reserly.platform.availability.dto;

import jakarta.validation.constraints.Min;

/** Payload privado para actualizar la capacidad máxima de una franja. */
public record TimeSlotCapacityRequest(@Min(1) int capacity) {}
