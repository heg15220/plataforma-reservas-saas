package com.reserly.platform.reservations.service;

/** La franja o selección solicitada no permite crear un hold. */
public class ReservationHoldInvalidException extends RuntimeException {
  public ReservationHoldInvalidException() {
    super("Reservation hold request is invalid");
  }
}
