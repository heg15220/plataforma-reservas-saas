package com.reserly.platform.demand.governance;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de la cola humana con bloqueo explícito para transiciones concurrentes. */
public interface DemandHumanReviewDao extends JpaRepository<DemandHumanReviewEntity, UUID> {
  /** Serializa dos primeras solicitudes con el mismo reviewId antes de que exista la fila. */
  @Query(
      value = "select pg_advisory_xact_lock(hashtextextended(cast(:id as text), 23))",
      nativeQuery = true)
  void lockSubmission(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select review from DemandHumanReviewEntity review where review.id = :id")
  Optional<DemandHumanReviewEntity> findByIdForUpdate(@Param("id") UUID id);

  @Query("select review from DemandHumanReviewEntity review order by review.updatedAt desc")
  List<DemandHumanReviewEntity> findAdminPage(Pageable pageable);

  @Query(
      "select review from DemandHumanReviewEntity review where review.venueId = :venueId order by review.updatedAt desc")
  List<DemandHumanReviewEntity> findVenuePage(@Param("venueId") UUID venueId, Pageable pageable);
}
