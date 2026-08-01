package com.reserly.platform.venues.dto;

/** Opción pública mínima de autocompletado obtenida exclusivamente de locales publicados. */
public record VenueSearchSuggestionResponse(
    String kind, String value, String label, String context) {}
