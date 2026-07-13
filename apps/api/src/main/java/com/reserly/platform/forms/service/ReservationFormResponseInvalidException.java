package com.reserly.platform.forms.service;

/** Rechaza una respuesta que no cumple el esquema vigente del formulario. */
public class ReservationFormResponseInvalidException extends RuntimeException {
  private final ReservationFormResponseViolation violation;
  private final String fieldKey;

  public ReservationFormResponseInvalidException(
      ReservationFormResponseViolation violation, String fieldKey) {
    super("Reservation form response is invalid");
    this.violation = violation;
    this.fieldKey = fieldKey;
  }

  public ReservationFormResponseViolation violation() {
    return violation;
  }

  public String fieldKey() {
    return fieldKey;
  }
}
