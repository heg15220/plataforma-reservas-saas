package com.reserly.platform.venues.dto;

/** Pestaña pública activa, ya localizada y saneada antes de llegar al cliente. */
public record VenuePublicCustomTabResponse(
    String title, String content, int position, String contentFormat) {}
