package com.reserly.platform.businessverification.service;

/** Rechazo genérico para impedir que una operación publique una cuenta no elegible. */
public class VenuePublicationNotAllowedException extends RuntimeException {

  public VenuePublicationNotAllowedException() {
    super("Venue publication is not allowed");
  }
}
