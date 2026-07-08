package com.reserly.platform.venues.service;

/** Se alcanza el límite editorial de pestañas por local definido para el MVP. */
public class VenueCustomTabLimitException extends RuntimeException {

  public VenueCustomTabLimitException() {
    super("Venue custom tab limit reached");
  }
}
