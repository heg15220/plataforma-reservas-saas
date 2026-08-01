package com.reserly.platform.venues.dto;

import java.util.List;

/** Resultado acotado de sugerencias para un locale y ámbito de búsqueda concretos. */
public record VenueSearchSuggestionsResponse(
    String locale, List<VenueSearchSuggestionResponse> suggestions) {}
