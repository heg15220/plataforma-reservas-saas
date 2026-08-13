package com.reserly.platform.demand.event.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso idempotente y temporal a eventos ya validados; no expone consultas sobre JSON libre. */
public interface BehaviorEventDao extends JpaRepository<BehaviorEventEntity, UUID> {

  /** Recupera el resultado persistido para responder de forma idempotente a un productor. */
  @Query("select event from BehaviorEventEntity event where event.eventId = :eventId")
  Optional<BehaviorEventEntity> findByEventId(@Param("eventId") UUID eventId);

  /**
   * Recupera un lote temporal estable por tipo usando ocurrencia y eventId como cursor conceptual.
   */
  @Query(
      """
      select event
      from BehaviorEventEntity event
      where event.eventType = :eventType
        and event.occurredAt >= :fromInclusive
        and event.occurredAt < :toExclusive
      order by event.occurredAt, event.eventId
      """)
  List<BehaviorEventEntity> findByTypeAndOccurredWindow(
      @Param("eventType") String eventType,
      @Param("fromInclusive") Instant fromInclusive,
      @Param("toExclusive") Instant toExclusive,
      Pageable pageable);

  /** Consulta eventos de un local sin inspeccionar ni indexar claves JSON ad hoc. */
  @Query(
      """
      select event
      from BehaviorEventEntity event
      where event.venueId = :venueId
        and event.occurredAt >= :fromInclusive
        and event.occurredAt < :toExclusive
      order by event.occurredAt, event.eventId
      """)
  List<BehaviorEventEntity> findByVenueAndOccurredWindow(
      @Param("venueId") UUID venueId,
      @Param("fromInclusive") Instant fromInclusive,
      @Param("toExclusive") Instant toExclusive,
      Pageable pageable);

  /** Lote acotado para la política de retención que se implementará en 19.18. */
  @Query(
      """
      select event
      from BehaviorEventEntity event
      where event.retentionExpiresAt <= :cutoff
      order by event.retentionExpiresAt, event.eventId
      """)
  List<BehaviorEventEntity> findRetentionExpired(
      @Param("cutoff") Instant cutoff, Pageable pageable);
}
