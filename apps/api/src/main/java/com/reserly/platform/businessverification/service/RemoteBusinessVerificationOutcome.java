package com.reserly.platform.businessverification.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Evidencia persistida devuelta al orquestador de estados futuro.
 *
 * @param verificationCheckId identificador del registro auditable
 * @param requestId identidad idempotente
 * @param providerCode proveedor seleccionado o marcador controlado
 * @param technicalStatus estado de check: verified, invalid, inconclusive o error
 * @param checkedAt instante del resultado
 * @param attemptCount invocaciones remotas efectuadas
 * @param durationMs duración total del gateway
 * @param businessVerificationStatus estado empresarial tras aplicar la evidencia
 * @param businessVerificationExpiresAt caducidad de la aprobación, si existe
 */
public record RemoteBusinessVerificationOutcome(
    UUID verificationCheckId,
    UUID requestId,
    String providerCode,
    String technicalStatus,
    Instant checkedAt,
    short attemptCount,
    int durationMs,
    String businessVerificationStatus,
    Instant businessVerificationExpiresAt) {

  public RemoteBusinessVerificationOutcome {
    Objects.requireNonNull(verificationCheckId);
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(providerCode);
    Objects.requireNonNull(technicalStatus);
    Objects.requireNonNull(checkedAt);
    Objects.requireNonNull(businessVerificationStatus);
  }
}
