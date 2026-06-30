package com.reserly.platform.identity.security;

import com.reserly.platform.identity.AccountType;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Principal inmutable disponible durante una petición autenticada.
 *
 * @param userId cuenta autenticada
 * @param sessionId sesión revocable que demostró la identidad
 * @param accountType naturaleza de cuenta, independiente de permisos
 * @param preferredLocale idioma persistido
 * @param roles concesiones explícitas usadas por Spring Security
 */
public record AuthenticatedAccount(
    UUID userId,
    UUID sessionId,
    AccountType accountType,
    String preferredLocale,
    Set<String> roles) {

  public AuthenticatedAccount {
    Objects.requireNonNull(userId);
    Objects.requireNonNull(sessionId);
    Objects.requireNonNull(accountType);
    Objects.requireNonNull(preferredLocale);
    roles = Set.copyOf(roles);
  }
}
