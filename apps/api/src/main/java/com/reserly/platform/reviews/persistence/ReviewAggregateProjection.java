package com.reserly.platform.reviews.persistence;

/**
 * Proyección escalar de valoración; evita cargar comentarios o identidades para calcular métricas.
 */
public interface ReviewAggregateProjection {

  /** Media exacta proporcionada por la base de datos o {@code null} cuando no existen filas. */
  Double getAverageRating();

  /** Número total de reseñas del local. */
  Long getReviewsCount();
}
