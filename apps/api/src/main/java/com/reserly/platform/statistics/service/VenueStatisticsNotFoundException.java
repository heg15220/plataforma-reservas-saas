package com.reserly.platform.statistics.service;

/** Oculta de forma uniforme propietario ausente y local inexistente o archivado. */
public class VenueStatisticsNotFoundException extends RuntimeException {

  public VenueStatisticsNotFoundException() {
    super("Venue statistics are unavailable");
  }
}
