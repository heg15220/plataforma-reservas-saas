package com.reserly.platform.venues.dto;

/** Imagen pública ordenada, sin identificadores internos ni metadatos de almacenamiento. */
public record VenuePublicGalleryImageResponse(String url, String altText, int position) {}
