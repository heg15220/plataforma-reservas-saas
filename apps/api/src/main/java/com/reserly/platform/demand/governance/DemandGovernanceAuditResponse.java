package com.reserly.platform.demand.governance;

import java.time.Instant;
import java.util.UUID;

/**
 * Confirmación opaca e idempotente; el detalle completo solo es visible en la API administrativa.
 */
public record DemandGovernanceAuditResponse(
    UUID auditLogId, UUID eventId, String resourceType, String action, Instant createdAt) {}
