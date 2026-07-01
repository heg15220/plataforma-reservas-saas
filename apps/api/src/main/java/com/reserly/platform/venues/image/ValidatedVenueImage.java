package com.reserly.platform.venues.image;

/** Imagen decodificada y re-codificada sin metadatos aportados por el cliente. */
public record ValidatedVenueImage(
    byte[] bytes, String mediaType, String extension, int width, int height) {

  public ValidatedVenueImage {
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
