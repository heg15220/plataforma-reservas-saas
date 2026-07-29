package com.reserly.platform.statistics.service;

/** Indica un periodo desconocido, incoherente, futuro o superior al máximo permitido. */
public class VenueStatisticsFilterInvalidException extends RuntimeException {

  public VenueStatisticsFilterInvalidException() {
    super("Venue statistics filter is invalid");
  }
}
