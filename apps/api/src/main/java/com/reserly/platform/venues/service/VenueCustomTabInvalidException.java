package com.reserly.platform.venues.service;

/** Error de validación de pestañas sin filtrar constraints ni contenido recibido. */
public class VenueCustomTabInvalidException extends RuntimeException {

  public VenueCustomTabInvalidException() {
    super("Invalid venue custom tab");
  }

  public VenueCustomTabInvalidException(Throwable cause) {
    super("Invalid venue custom tab", cause);
  }
}
