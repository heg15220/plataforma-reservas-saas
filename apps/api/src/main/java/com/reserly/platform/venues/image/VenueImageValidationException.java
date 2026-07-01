package com.reserly.platform.venues.image;

/** Rechazo genérico de contenido que no cumple el contrato de imagen segura. */
public class VenueImageValidationException extends RuntimeException {

  public VenueImageValidationException() {
    super("La imagen principal no cumple el contrato de carga.");
  }
}
