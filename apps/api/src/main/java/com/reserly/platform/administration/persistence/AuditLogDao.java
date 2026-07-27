package com.reserly.platform.administration.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia append-only de evidencia de auditoría. */
public interface AuditLogDao extends JpaRepository<AuditLogEntity, UUID> {}
