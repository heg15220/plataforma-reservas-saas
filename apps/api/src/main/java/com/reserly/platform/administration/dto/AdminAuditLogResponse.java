package com.reserly.platform.administration.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Evidencia visible minimizada; omite IP y user-agent de la respuesta inicial. */
public record AdminAuditLogResponse(
    UUID id,
    UUID actorUserId,
    String actorRole,
    String entityType,
    UUID entityId,
    String action,
    Map<String, Object> before,
    Map<String, Object> after,
    Instant createdAt) {}
