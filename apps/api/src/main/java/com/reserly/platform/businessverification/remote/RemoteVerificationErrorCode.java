package com.reserly.platform.businessverification.remote;

/**
 * Taxonomía controlada de fallos remotos.
 *
 * <p>Cada código fija si es seguro reintentar y la clave i18n que puede persistirse. Los mensajes
 * originales del proveedor no atraviesan esta frontera.
 */
public enum RemoteVerificationErrorCode {
  NO_ADAPTER_CONFIGURED(false, "businessVerification.remote.noAdapter"),
  PROVIDER_TIMEOUT(true, "businessVerification.remote.timeout"),
  PROVIDER_UNAVAILABLE(true, "businessVerification.remote.unavailable"),
  PROVIDER_RATE_LIMITED(true, "businessVerification.remote.rateLimited"),
  PROVIDER_AUTHENTICATION_ERROR(false, "businessVerification.remote.authentication"),
  PROVIDER_PROTOCOL_ERROR(false, "businessVerification.remote.protocol"),
  INVALID_PROVIDER_RESPONSE(false, "businessVerification.remote.invalidResponse");

  private final boolean retryable;
  private final String messageKey;

  RemoteVerificationErrorCode(boolean retryable, String messageKey) {
    this.retryable = retryable;
    this.messageKey = messageKey;
  }

  public boolean retryable() {
    return retryable;
  }

  public String messageKey() {
    return messageKey;
  }
}
