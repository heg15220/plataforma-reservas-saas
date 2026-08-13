package com.reserly.platform.demand.privacy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Resultado opaco, minimizado e idempotente de un derecho atendido. */
public record DemandPrivacyResponse(
    UUID requestId,
    String status,
    String action,
    Map<String, Object> result,
    Instant completedAt) {}
