package com.reserly.platform.identity.dto;

import java.time.Instant;

/**
 * Confirmación pública de una verificación completada.
 *
 * @param emailVerified valor verdadero tras el consumo correcto
 * @param emailVerifiedAt instante UTC definitivo
 * @param accountStatus estado operativo resultante
 */
public record EmailVerificationResponse(
    boolean emailVerified, Instant emailVerifiedAt, String accountStatus) {}
