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
 */
public record RemoteBusinessVerificationOutcome(
    UUID verificationCheckId,
    UUID requestId,
    String providerCode,
    String technicalStatus,
    Instant checkedAt,
    short attemptCount,
    int durationMs) {

  public RemoteBusinessVerificationOutcome {
    Objects.requireNonNull(verificationCheckId);
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(providerCode);
    Objects.requireNonNull(technicalStatus);
    Objects.requireNonNull(checkedAt);
  }
}
