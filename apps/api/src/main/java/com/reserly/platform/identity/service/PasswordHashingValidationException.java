package com.reserly.platform.identity.service;

/** Entrada de contraseña que BCrypt no puede procesar sin truncamiento o ambigüedad. */
public class PasswordHashingValidationException extends RuntimeException {

  public PasswordHashingValidationException() {
    super("Password input violates hashing constraints");
  }
}
