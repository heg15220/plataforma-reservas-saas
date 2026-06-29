package com.reserly.platform.identity.service;

/** Rechazo uniforme de credencial, tipo o estado sin revelar cuál falló. */
public class InvalidAuthenticationException extends RuntimeException {

  public InvalidAuthenticationException() {
    super("Authentication failed");
  }
}
