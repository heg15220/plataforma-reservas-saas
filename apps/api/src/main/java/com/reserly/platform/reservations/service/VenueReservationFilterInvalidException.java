package com.reserly.platform.reservations.service;

/** Indica que un filtro del panel no puede convertirse a una consulta segura y acotada. */
public class VenueReservationFilterInvalidException extends RuntimeException {

  public VenueReservationFilterInvalidException() {
    super("Invalid venue reservation filter");
  }
}
