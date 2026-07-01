package com.reserly.platform.venues.service;

/** Indica que la galería ya alcanzó el máximo MVP de ocho imágenes. */
public class VenueGalleryLimitException extends RuntimeException {

  public VenueGalleryLimitException() {
    super("La galería ha alcanzado su límite.");
  }
}
