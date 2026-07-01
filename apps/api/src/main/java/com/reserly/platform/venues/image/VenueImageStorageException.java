package com.reserly.platform.venues.image;

/** Señala indisponibilidad del almacenamiento sin revelar proveedor, bucket o clave. */
public class VenueImageStorageException extends RuntimeException {

  public VenueImageStorageException() {
    super("El almacenamiento de imágenes no está disponible.");
  }
}
