package com.reserly.platform.reviews.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Confirmación minimizada de una reseña creada.
 *
 * <p>No expone el email ni datos históricos de la reserva. Incluye el agregado del local calculado
 * inmediatamente después de persistir la reseña para refrescar el resumen sin una segunda petición.
 */
public record ReviewCreateResponse(
    String status,
    UUID reviewId,
    UUID venueId,
    UUID reservationId,
    int rating,
    BigDecimal averageRating,
    long reviewsCount) {}
