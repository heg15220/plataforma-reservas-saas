package com.reserly.platform.demand.privacy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/** Comando interno; correction acepta HMAC ya calculado, nunca email en claro. */
public record DemandPrivacyRequest(
    @NotNull UUID requestId,
    @NotNull UUID subjectId,
    @NotNull @Pattern(regexp = "anonymous|customer") String subjectType,
    @NotNull @Pattern(regexp = "access|correction|objection|revocation|unlink|erasure")
        String action,
    @Pattern(regexp = "analytics|personalization|experimentation|commercial_activation")
        String purpose,
    @Pattern(regexp = "^[0-9a-f]{64}$") String replacementEmailHmac,
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$") String replacementKeyVersion) {}
