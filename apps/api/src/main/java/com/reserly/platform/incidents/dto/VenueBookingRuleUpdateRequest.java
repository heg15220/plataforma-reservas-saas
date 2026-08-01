package com.reserly.platform.incidents.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Reemplazo de las reglas básicas de cancelación del local autenticado.
 *
 * @param cancellationAllowed habilita la cancelación pública mediante el enlace seguro
 * @param freeCancellationUntilMinutesBefore antelación mínima inclusiva respecto al inicio
 */
public record VenueBookingRuleUpdateRequest(
    boolean cancellationAllowed, @Min(0) @Max(525600) int freeCancellationUntilMinutesBefore) {}
