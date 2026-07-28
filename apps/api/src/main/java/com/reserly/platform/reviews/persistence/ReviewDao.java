package com.reserly.platform.reviews.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de reseñas con consultas explícitas para las invariantes de creación. */
public interface ReviewDao extends JpaRepository<ReviewEntity, UUID> {

  /** Comprueba la unicidad funcional antes de insertar; la base de datos conserva la defensa final. */
  boolean existsByReservationId(UUID reservationId);
}
