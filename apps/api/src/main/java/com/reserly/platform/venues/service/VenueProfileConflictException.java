package com.reserly.platform.venues.service;

/** Conflicto de perfil vigente o identidad generada, sin detalles de constraint. */
public class VenueProfileConflictException extends RuntimeException {

  public VenueProfileConflictException() {}

  public VenueProfileConflictException(Throwable cause) {
    super(cause);
  }
}
