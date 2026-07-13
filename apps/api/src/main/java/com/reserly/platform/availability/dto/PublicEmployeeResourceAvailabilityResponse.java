package com.reserly.platform.availability.dto;

import java.util.UUID;

/**
 * Identidad publica minima de un empleado o recurso elegible para una franja.
 *
 * <p>No expone nombre legal, notas internas, estado administrativo ni datos del propietario.
 */
public record PublicEmployeeResourceAvailabilityResponse(
    UUID employeeResourceId, String type, String displayName, String specialty) {}
