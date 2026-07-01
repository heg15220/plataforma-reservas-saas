package com.reserly.platform.venues.service;

/** Bytes públicos ya normalizados junto con su MIME confiable. */
public record VenueMainImageContent(byte[] bytes, String mediaType) {

  public VenueMainImageContent {
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
