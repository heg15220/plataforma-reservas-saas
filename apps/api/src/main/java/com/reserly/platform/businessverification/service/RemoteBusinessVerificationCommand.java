package com.reserly.platform.businessverification.service;

import java.util.Objects;
import java.util.UUID;

/**
 * Orden interna para comprobar una cuenta ya persistida.
 *
 * @param requestId identidad idempotente generada por el caso de uso o job llamante
 * @param businessAccountId cuenta empresarial que debe cargarse desde la fuente de verdad
 * @param preferredProvider proveedor opcional seleccionado por política administrativa
 */
public record RemoteBusinessVerificationCommand(
    UUID requestId, UUID businessAccountId, String preferredProvider) {

  public RemoteBusinessVerificationCommand {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(businessAccountId);
    if (preferredProvider != null && preferredProvider.length() > 64) {
      throw new IllegalArgumentException("Preferred provider code is too long");
    }
  }
}
