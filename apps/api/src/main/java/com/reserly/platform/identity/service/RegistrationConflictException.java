package com.reserly.platform.identity.service;

/** Conflicto genérico de alta que evita revelar qué identidad concreta ya existe. */
public class RegistrationConflictException extends RuntimeException {

  public RegistrationConflictException() {
    super("Registration conflicts with an existing identity");
  }

  public RegistrationConflictException(Throwable cause) {
    super("Registration conflicts with an existing identity", cause);
  }
}
