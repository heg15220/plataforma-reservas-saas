package com.reserly.platform.businessverification.service;

/**
 * Protege la idempotencia cuando un request ya pertenece a otra cuenta empresarial.
 *
 * <p>No expone ninguno de los identificadores implicados.
 */
public class RemoteVerificationRequestConflictException extends RuntimeException {

  public RemoteVerificationRequestConflictException() {
    super("Remote verification request belongs to a different business account");
  }
}
