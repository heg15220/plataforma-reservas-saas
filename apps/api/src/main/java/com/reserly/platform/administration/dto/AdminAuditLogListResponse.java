package com.reserly.platform.administration.dto;

import java.util.List;

/** Últimas acciones críticas visibles para administración. */
public record AdminAuditLogListResponse(List<AdminAuditLogResponse> logs) {}
