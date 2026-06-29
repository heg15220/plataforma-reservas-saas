package com.reserly.platform.identity.service;

/** Señala un desafío inexistente, expirado, consumido, revocado o no admisible. */
public class InvalidEmailVerificationException extends RuntimeException {

  public InvalidEmailVerificationException() {
    super("Email verification challenge is invalid");
  }
}
