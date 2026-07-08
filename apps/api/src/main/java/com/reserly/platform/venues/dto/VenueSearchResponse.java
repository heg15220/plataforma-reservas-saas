package com.reserly.platform.venues.dto;

import java.util.List;

/** Página pública localizada de locales publicados preparada para tarjetas de búsqueda. */
public record VenueSearchResponse(
    String locale,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    List<VenueSearchItemResponse> results) {}
