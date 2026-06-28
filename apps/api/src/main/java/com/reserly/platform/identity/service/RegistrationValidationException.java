package com.reserly.platform.identity.service;

/** Rechazo de una invariante de registro que no puede expresarse solo con Bean Validation. */
public class RegistrationValidationException extends RuntimeException {

  public RegistrationValidationException() {
    super("Registration data violates a security invariant");
  }
}
