package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminAuditLogListResponse;

/** Consulta visible y acotada de la evidencia append-only. */
public interface AdminAuditQueryService {
  AdminAuditLogListResponse listRecent();
}
