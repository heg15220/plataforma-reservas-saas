package com.reserly.platform.reservations.dto;

import java.util.List;

/**
 * Página estable del panel privado de reservas.
 *
 * @param items reservas de la página actual
 * @param page índice de página basado en cero
 * @param size tamaño máximo solicitado
 * @param totalElements total coincidente con los filtros
 * @param totalPages número total de páginas
 */
public record VenueReservationListResponse(
    List<VenueReservationSummaryResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages) {

  public VenueReservationListResponse {
    items = List.copyOf(items);
  }
}
