package com.reserly.platform.reservations.service;

/**
 * Ausencia opaca de una reserva propia; también representa identificadores pertenecientes a otro
 * local.
 */
public class VenueReservationNotFoundException extends RuntimeException {

  public VenueReservationNotFoundException() {
    super("Venue reservation not found");
  }
}
