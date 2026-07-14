package com.reserly.platform.reservations.service;

/** Indica al poseedor autenticado por token que el límite exclusivo del hold ya venció. */
public class ReservationHoldExpiredException extends RuntimeException {

  public ReservationHoldExpiredException() {
    super("Reservation hold has expired");
  }
}
