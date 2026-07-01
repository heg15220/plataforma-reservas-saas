package com.reserly.platform.venues.dto;

import java.util.UUID;

/** Imagen de galería sin clave de almacenamiento. */
public record VenueGalleryImageResponse(
    UUID id,
    String url,
    String altText,
    int position,
    String mediaType,
    long sizeBytes,
    int width,
    int height) {}
