package com.reserly.platform.venues.image;

/** Puerto de objetos de imagen; las claves son internas y el bucket permanece privado. */
public interface VenueImageStorage {

  void put(String objectKey, byte[] content, String mediaType);

  byte[] get(String objectKey);

  void delete(String objectKey);
}
