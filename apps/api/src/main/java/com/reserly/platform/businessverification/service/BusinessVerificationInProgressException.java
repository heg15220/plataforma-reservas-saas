package com.reserly.platform.businessverification.service;

/** Impide solapar dos verificaciones remotas sobre la misma identidad empresarial. */
public class BusinessVerificationInProgressException extends RuntimeException {

  public BusinessVerificationInProgressException() {
    super("Business verification is already in progress");
  }
}
