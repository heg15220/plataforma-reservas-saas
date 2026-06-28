package com.reserly.platform.businessverification.service;

/** Señala evidencia tardía, ajena o incompatible con la operación activa. */
public class BusinessVerificationStateConflictException extends RuntimeException {

  public BusinessVerificationStateConflictException() {
    super("Business verification state transition conflict");
  }
}
