package com.reserly.platform.demand.identity;

import java.time.Instant;
import java.util.UUID;

/** Proyección minimizada; omite deliberadamente email y HMAC. */
public record ProgressiveIdentityResult(
    UUID linkId,
    UUID sessionId,
    UUID anonymousIdentityId,
    UUID customerIdentityId,
    String keyVersion,
    String purpose,
    boolean keyRotated,
    Instant linkedAt) {}
