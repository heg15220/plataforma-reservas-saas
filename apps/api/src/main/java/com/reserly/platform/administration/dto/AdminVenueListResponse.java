package com.reserly.platform.administration.dto;

import java.util.List;

/** Primer tramo acotado del listado administrativo de locales. */
public record AdminVenueListResponse(List<AdminVenueResponse> venues) {
  public AdminVenueListResponse {
    venues = List.copyOf(venues);
  }
}
