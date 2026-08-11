package com.reserly.platform.administration.service;

import com.reserly.platform.administration.persistence.AuditLogDao;
import com.reserly.platform.administration.persistence.AuditLogEntity;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valida y persiste entradas de auditoría sin registrar su contenido en logs de aplicación. */
@Service
public class AuditLogServiceImpl implements AuditLogService {

  private static final int MAX_IP_LENGTH = 45;
  private static final int MAX_USER_AGENT_LENGTH = 500;
  private static final Set<String> HUMAN_ACTOR_ROLES = Set.of("venue_owner", "admin");

  private final AuditLogDao auditLogDao;
  private final Clock clock;

  public AuditLogServiceImpl(AuditLogDao auditLogDao, Clock clock) {
    this.auditLogDao = auditLogDao;
    this.clock = clock;
  }

  @Override
  @Transactional
  public AuditLogEntity record(AuditLogEntry entry) {
    validate(entry);
    AuditLogEntity auditLog = new AuditLogEntity();
    auditLog.setActorUserId(entry.actorUserId());
    auditLog.setActorRole(entry.actorRole());
    auditLog.setEntityType(entry.entityType().trim());
    auditLog.setEntityId(entry.entityId());
    auditLog.setAction(entry.action().trim());
    auditLog.setBeforeJson(copySnapshot(entry.beforeJson()));
    auditLog.setAfterJson(copySnapshot(entry.afterJson()));
    auditLog.setIpAddress(normalizeOptional(entry.ipAddress(), MAX_IP_LENGTH));
    auditLog.setUserAgent(normalizeOptional(entry.userAgent(), MAX_USER_AGENT_LENGTH));
    auditLog.setCreatedAt(clock.instant());
    return auditLogDao.saveAndFlush(auditLog);
  }

  private void validate(AuditLogEntry entry) {
    if (entry == null
        || !validActor(entry)
        || entry.entityId() == null
        || isBlank(entry.entityType())
        || entry.entityType().length() > 64
        || isBlank(entry.action())
        || entry.action().length() > 64) {
      throw new IllegalArgumentException("Invalid audit log entry");
    }
  }

  private boolean validActor(AuditLogEntry entry) {
    return (entry.actorUserId() != null && HUMAN_ACTOR_ROLES.contains(entry.actorRole()))
        || (entry.actorUserId() == null && "system".equals(entry.actorRole()));
  }

  private Map<String, Object> copySnapshot(Map<String, Object> snapshot) {
    return snapshot == null ? null : Map.copyOf(snapshot);
  }

  private String normalizeOptional(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    return normalized.substring(0, Math.min(normalized.length(), maxLength));
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
