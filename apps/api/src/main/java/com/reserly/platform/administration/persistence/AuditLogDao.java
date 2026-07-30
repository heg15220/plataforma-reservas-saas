package com.reserly.platform.administration.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Persistencia append-only de evidencia de auditoría. */
public interface AuditLogDao extends JpaRepository<AuditLogEntity, UUID> {

  /** Últimas acciones críticas con límite impuesto por el servicio. */
  @Query("select log from AuditLogEntity log order by log.createdAt desc, log.id desc")
  List<AuditLogEntity> findAdminPage(Pageable pageable);
}
