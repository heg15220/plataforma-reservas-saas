package com.reserly.platform.businessverification.remote;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadatos de transporte estables o acotados para una invocación.
 *
 * @param requestId correlación local de la operación
 * @param idempotencyKey hash opaco y estable que el adaptador debe propagar cuando el proveedor lo
 *     admita
 * @param attemptNumber número de intento empezando en uno
 * @param connectTimeout límite de establecimiento de conexión
 * @param readTimeout límite de espera de respuesta
 */
public record RemoteVerificationAttemptContext(
    UUID requestId,
    String idempotencyKey,
    int attemptNumber,
    Duration connectTimeout,
    Duration readTimeout) {

  public RemoteVerificationAttemptContext {
    Objects.requireNonNull(requestId);
    if (idempotencyKey == null || !idempotencyKey.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Idempotency key must be an opaque SHA-256 value");
    }
    if (attemptNumber < 1) {
      throw new IllegalArgumentException("Attempt number must be positive");
    }
    if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
      throw new IllegalArgumentException("Connect timeout must be positive");
    }
    if (readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
      throw new IllegalArgumentException("Read timeout must be positive");
    }
  }
}
