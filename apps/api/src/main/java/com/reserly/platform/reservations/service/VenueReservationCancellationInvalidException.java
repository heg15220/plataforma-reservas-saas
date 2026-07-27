package com.reserly.platform.reservations.service;

/** Motivo ausente, excesivo o transición no admisible para cancelación preventiva. */
public class VenueReservationCancellationInvalidException extends RuntimeException {

  public VenueReservationCancellationInvalidException() {
    super("Venue reservation cancellation is invalid");
  }
}
