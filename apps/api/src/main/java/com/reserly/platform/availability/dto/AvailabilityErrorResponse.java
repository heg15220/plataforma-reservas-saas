package com.reserly.platform.availability.dto;

/** Error estable de disponibilidad que no expone IDs ajenos ni constraints internas. */
public record AvailabilityErrorResponse(String error) {}
