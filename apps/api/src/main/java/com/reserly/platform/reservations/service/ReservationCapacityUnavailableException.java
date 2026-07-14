package com.reserly.platform.reservations.service;

/** Indica que la ocupación autoritativa ya no permite confirmar el partySize retenido. */
public class ReservationCapacityUnavailableException extends RuntimeException {

  public ReservationCapacityUnavailableException() {
    super("Reservation capacity is unavailable");
  }
}
