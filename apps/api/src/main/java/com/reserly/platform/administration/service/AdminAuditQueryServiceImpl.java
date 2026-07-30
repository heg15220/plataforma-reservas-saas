package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminAuditLogListResponse;
import com.reserly.platform.administration.dto.AdminAuditLogResponse;
import com.reserly.platform.administration.persistence.AuditLogDao;
import com.reserly.platform.administration.persistence.AuditLogEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Proyecta auditoría sin IP ni user-agent para minimizar datos personales en la vista inicial. */
@Service
public class AdminAuditQueryServiceImpl implements AdminAuditQueryService {
  static final int LIST_LIMIT = 100;
  private final AuditLogDao auditLogDao;

  public AdminAuditQueryServiceImpl(AuditLogDao auditLogDao) {
    this.auditLogDao = auditLogDao;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminAuditLogListResponse listRecent() {
    return new AdminAuditLogListResponse(
        auditLogDao.findAdminPage(PageRequest.of(0, LIST_LIMIT)).stream()
            .map(this::response)
            .toList());
  }

  private AdminAuditLogResponse response(AuditLogEntity log) {
    return new AdminAuditLogResponse(
        log.getId(),
        log.getActorUserId(),
        log.getActorRole(),
        log.getEntityType(),
        log.getEntityId(),
        log.getAction(),
        log.getBeforeJson(),
        log.getAfterJson(),
        log.getCreatedAt());
  }
}
