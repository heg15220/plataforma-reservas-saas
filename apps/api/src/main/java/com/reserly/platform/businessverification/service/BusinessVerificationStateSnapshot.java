package com.reserly.platform.businessverification.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Resumen inmutable del estado empresarial tras una transición.
 *
 * @param businessAccountId cuenta afectada
 * @param status estado persistido
 * @param verifiedAt instante de aprobación vigente o histórica
 * @param expiresAt caducidad de la aprobación o {@code null}
 */
public record BusinessVerificationStateSnapshot(
    UUID businessAccountId,
    BusinessVerificationStatus status,
    Instant verifiedAt,
    Instant expiresAt) {

  public BusinessVerificationStateSnapshot {
    Objects.requireNonNull(businessAccountId);
    Objects.requireNonNull(status);
  }
}
