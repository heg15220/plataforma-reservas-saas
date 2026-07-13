package com.reserly.platform.forms.service;

/** Indica que un campo no cumple el contrato funcional o las restricciones persistidas. */
public class ReservationFormFieldInvalidException extends RuntimeException {
  public ReservationFormFieldInvalidException() {}

  public ReservationFormFieldInvalidException(Throwable cause) {
    super(cause);
  }
}
