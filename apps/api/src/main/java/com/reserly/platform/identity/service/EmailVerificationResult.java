package com.reserly.platform.identity.service;

import java.time.Instant;

/**
 * Resultado interno no sensible de consumir un desafío.
 *
 * @param verifiedAt instante definitivo de verificación
 * @param accountStatus estado operativo resultante de la cuenta
 */
public record EmailVerificationResult(Instant verifiedAt, String accountStatus) {}
