package com.reserly.platform.reservations.dto;

import java.util.UUID;

/**
 * Empleado o recurso asignado visible únicamente para el propietario del local.
 *
 * <p>No incluye notas internas, descripción, visibilidad pública ni datos del local.
 */
public record VenueReservationAssignedResourceResponse(
    UUID id,
    String type,
    String firstName,
    String lastName,
    String publicAlias,
    String specialty,
    String status) {}
