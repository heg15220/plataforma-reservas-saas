package com.reserly.platform.reservations.service;

/** Ausencia opaca de una reserva propia cancelable. */
public class VenueReservationCancellationNotFoundException extends RuntimeException {

  public VenueReservationCancellationNotFoundException() {
    super("Venue reservation cancellation target not found");
  }
}
