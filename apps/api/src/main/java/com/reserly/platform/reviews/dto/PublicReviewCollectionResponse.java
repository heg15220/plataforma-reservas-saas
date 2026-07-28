package com.reserly.platform.reviews.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Colección pública acotada con su valoración agregada.
 *
 * @param averageRating media redondeada a una cifra o {@code null} sin reseñas
 * @param reviewsCount total real de reseñas
 * @param truncated indica si existen más reseñas que las incluidas
 * @param items tramo reciente sin identidad
 */
public record PublicReviewCollectionResponse(
    BigDecimal averageRating,
    long reviewsCount,
    boolean truncated,
    List<ReviewItemResponse> items) {

  public PublicReviewCollectionResponse {
    items = List.copyOf(items);
  }
}
