package com.reserly.platform.identity.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado interno de login.
 *
 * <p>El token solo lo consume el adaptador de cookie y nunca forma parte del DTO JSON.
 */
public record LoginOutcome(
    String sessionToken,
    Instant sessionExpiresAt,
    UUID userId,
    String accountType,
    String preferredLocale,
    boolean emailVerified) {

  public LoginOutcome {
    Objects.requireNonNull(sessionToken);
    Objects.requireNonNull(sessionExpiresAt);
    Objects.requireNonNull(userId);
    Objects.requireNonNull(accountType);
    Objects.requireNonNull(preferredLocale);
  }
}
