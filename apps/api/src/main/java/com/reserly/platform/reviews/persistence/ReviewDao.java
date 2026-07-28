package com.reserly.platform.reviews.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de reseñas con consultas explícitas para las invariantes de creación. */
public interface ReviewDao extends JpaRepository<ReviewEntity, UUID> {

  /**
   * Comprueba la unicidad funcional antes de insertar; la base de datos conserva la defensa final.
   */
  boolean existsByReservationId(UUID reservationId);

  /** Calcula media y total en una sola lectura de la tabla para un local concreto. */
  @Query(
      """
      select avg(review.rating) as averageRating, count(review.id) as reviewsCount
      from ReviewEntity review
      where review.venueId = :venueId
      """)
  ReviewAggregateProjection summarizeByVenueId(@Param("venueId") UUID venueId);

  /** Lista reseñas con orden estable, reutilizable por la ficha y el panel privado. */
  Page<ReviewEntity> findByVenueIdOrderByCreatedAtDescIdDesc(UUID venueId, Pageable pageable);
}
