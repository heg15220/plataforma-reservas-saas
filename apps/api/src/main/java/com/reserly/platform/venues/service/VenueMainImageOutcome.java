package com.reserly.platform.venues.service;

/** Proyección del objeto normalizado persistido como imagen principal. */
public record VenueMainImageOutcome(
    String url, String mediaType, long sizeBytes, int width, int height) {}
