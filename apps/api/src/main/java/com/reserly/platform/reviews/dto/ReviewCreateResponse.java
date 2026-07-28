package com.reserly.platform.reviews.dto;

import java.util.UUID;

/**
 * Confirmación minimizada de una reseña creada.
 *
 * <p>No expone el email ni datos históricos de la reserva. La agregación de valoración se añadirá
 * en la tarea específica de métricas de reseñas.
 */
public record ReviewCreateResponse(
    String status, UUID reviewId, UUID venueId, UUID reservationId, int rating) {}
