package com.reserly.platform.identity.dto;

import java.util.UUID;

/**
 * Resultado no sensible de un registro empresarial.
 *
 * @param userId cuenta creada
 * @param businessAccountId identidad empresarial creada
 * @param accountType tipo fijado por backend
 * @param businessVerificationStatus estado previo a comprobación remota
 * @param emailVerificationRequired indica que el email sigue pendiente
 * @param canPublishVenue siempre falso en este punto
 */
public record VenueRegistrationResponse(
    UUID userId,
    UUID businessAccountId,
    String accountType,
    String businessVerificationStatus,
    boolean emailVerificationRequired,
    boolean canPublishVenue) {}
