package com.reserly.platform.identity.service;

/** Agrupa token, cuenta o contraseña no admisibles sin revelar la causa pública. */
public class InvalidPasswordResetException extends RuntimeException {

  public InvalidPasswordResetException() {
    super("Password reset request is invalid");
  }
}
