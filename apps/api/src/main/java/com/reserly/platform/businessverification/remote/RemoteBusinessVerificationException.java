package com.reserly.platform.businessverification.remote;

import java.util.Objects;

/**
 * Fallo normalizado producido por un adaptador.
 *
 * <p>No acepta mensajes ni payloads remotos para que datos fiscales o secretos del proveedor no
 * terminen accidentalmente en logs.
 */
public class RemoteBusinessVerificationException extends Exception {

  private final RemoteVerificationErrorCode errorCode;

  public RemoteBusinessVerificationException(RemoteVerificationErrorCode errorCode) {
    super("Remote business verification failed: " + Objects.requireNonNull(errorCode).name());
    this.errorCode = errorCode;
  }

  public RemoteVerificationErrorCode getErrorCode() {
    return errorCode;
  }
}
