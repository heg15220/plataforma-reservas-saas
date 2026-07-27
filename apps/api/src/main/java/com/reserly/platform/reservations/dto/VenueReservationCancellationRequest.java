package com.reserly.platform.reservations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo profesional obligatorio para una cancelación iniciada por el local.
 *
 * <p>Se persiste y audita como texto plano; el email lo renderiza escapado.
 */
public record VenueReservationCancellationRequest(
    @NotBlank @Size(max = 500) String reason) {}
