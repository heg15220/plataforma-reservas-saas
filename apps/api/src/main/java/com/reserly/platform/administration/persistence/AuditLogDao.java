package com.reserly.platform.administration.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia append-only de evidencia de auditoría. */
public interface AuditLogDao extends JpaRepository<AuditLogEntity, UUID> {

  /** Últimas acciones críticas con límite impuesto por el servicio. */
  @Query("select log from AuditLogEntity log order by log.createdAt desc, log.id desc")
  List<AuditLogEntity> findAdminPage(Pageable pageable);

  /** Resuelve un eventId de gobierno para que un reintento idéntico no duplique evidencia. */
  Optional<AuditLogEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId);

  /** Serializa reintentos del eventId incluso antes de que exista la primera fila. */
  @Query(
      value = "select pg_advisory_xact_lock(hashtextextended(cast(:eventId as text), 0))",
      nativeQuery = true)
  void lockDemandGovernanceEvent(@Param("eventId") UUID eventId);
}
