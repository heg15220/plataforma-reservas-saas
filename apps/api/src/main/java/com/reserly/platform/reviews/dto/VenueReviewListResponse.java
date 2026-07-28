package com.reserly.platform.reviews.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Página privada de reseñas del local autenticado con métricas coherentes.
 *
 * @param averageRating media redondeada a una cifra o {@code null} sin reseñas
 * @param reviewsCount total real recibido por el local
 * @param items reseñas de la página
 * @param page índice basado en cero
 * @param size tamaño máximo solicitado
 * @param totalPages número total de páginas
 */
public record VenueReviewListResponse(
    BigDecimal averageRating,
    long reviewsCount,
    List<ReviewItemResponse> items,
    int page,
    int size,
    int totalPages) {

  public VenueReviewListResponse {
    items = List.copyOf(items);
  }
}
