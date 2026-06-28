package com.reserly.platform.businessverification.remote;

import java.util.Objects;
import java.util.UUID;

/**
 * Resultado del gateway con metadatos operativos seguros.
 *
 * @param requestId identidad idempotente
 * @param providerCode adaptador seleccionado
 * @param result resultado minimizado
 * @param attemptCount llamadas efectuadas
 * @param durationMs duración total, incluidos backoffs
 */
public record RemoteVerificationExecution(
    UUID requestId,
    String providerCode,
    RemoteBusinessVerificationResult result,
    short attemptCount,
    int durationMs) {

  public RemoteVerificationExecution {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(providerCode);
    Objects.requireNonNull(result);
    if (attemptCount < 1 || durationMs < 0) {
      throw new IllegalArgumentException("Remote execution metadata is invalid");
    }
  }
}
