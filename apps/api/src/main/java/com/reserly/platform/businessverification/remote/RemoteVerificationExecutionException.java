package com.reserly.platform.businessverification.remote;

import java.util.Objects;
import java.util.UUID;

/** Fallo final del gateway después de selección, timeout y política de reintentos. */
public class RemoteVerificationExecutionException extends RuntimeException {

  private final UUID requestId;
  private final String providerCode;
  private final RemoteVerificationErrorCode errorCode;
  private final short attemptCount;
  private final int durationMs;

  public RemoteVerificationExecutionException(
      UUID requestId,
      String providerCode,
      RemoteVerificationErrorCode errorCode,
      short attemptCount,
      int durationMs) {
    super("Remote verification execution failed: " + Objects.requireNonNull(errorCode).name());
    this.requestId = Objects.requireNonNull(requestId);
    this.providerCode = Objects.requireNonNull(providerCode);
    this.errorCode = errorCode;
    this.attemptCount = attemptCount;
    this.durationMs = durationMs;
  }

  public UUID getRequestId() {
    return requestId;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public RemoteVerificationErrorCode getErrorCode() {
    return errorCode;
  }

  public short getAttemptCount() {
    return attemptCount;
  }

  public int getDurationMs() {
    return durationMs;
  }
}
