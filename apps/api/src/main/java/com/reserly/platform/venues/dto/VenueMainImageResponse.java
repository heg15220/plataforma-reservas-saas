package com.reserly.platform.venues.dto;

/** Resultado seguro de una carga, sin bucket, credenciales ni clave interna. */
public record VenueMainImageResponse(
    String url, String mediaType, long sizeBytes, int width, int height) {}
