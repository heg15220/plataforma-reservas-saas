package com.reserly.platform.reviews.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Confirmación pública de creación desde la ficha, deliberadamente sin identificador de reserva.
 */
public record PublicVenueReviewCreateResponse(
    String status,
    UUID reviewId,
    UUID venueId,
    int rating,
    BigDecimal averageRating,
    long reviewsCount) {

  public static PublicVenueReviewCreateResponse from(ReviewCreateResponse response) {
    return new PublicVenueReviewCreateResponse(
        response.status(),
        response.reviewId(),
        response.venueId(),
        response.rating(),
        response.averageRating(),
        response.reviewsCount());
  }
}
