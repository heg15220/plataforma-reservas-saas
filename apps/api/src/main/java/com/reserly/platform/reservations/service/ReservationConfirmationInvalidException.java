package com.reserly.platform.reservations.service;

/** Rechazo público uniforme ante una confirmación inválida o no autorizada por el hold. */
public class ReservationConfirmationInvalidException extends RuntimeException {

  public ReservationConfirmationInvalidException() {
    super("Reservation confirmation is invalid");
  }
}
