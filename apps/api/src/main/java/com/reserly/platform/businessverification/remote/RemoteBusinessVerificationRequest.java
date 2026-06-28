package com.reserly.platform.businessverification.remote;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos mínimos entregados a un adaptador remoto.
 *
 * @param requestId identidad idempotente de la operación lógica
 * @param businessAccountId cuenta interna, nunca enviada al proveedor salvo necesidad documentada
 * @param taxCountry país fiscal canónico
 * @param taxIdentifier identificador fiscal canónico
 * @param legalName razón social aportada para contrastes que el proveedor soporte
 * @param address dirección opcional para contraste
 */
public record RemoteBusinessVerificationRequest(
    UUID requestId,
    UUID businessAccountId,
    String taxCountry,
    String taxIdentifier,
    String legalName,
    String address) {

  public RemoteBusinessVerificationRequest {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(businessAccountId);
    if (taxCountry == null || !taxCountry.matches("[A-Z]{2}")) {
      throw new IllegalArgumentException("Tax country must be canonical");
    }
    if (taxIdentifier == null || taxIdentifier.isBlank() || taxIdentifier.length() > 64) {
      throw new IllegalArgumentException("Tax identifier must be canonical");
    }
    if (legalName == null || legalName.isBlank() || legalName.length() > 255) {
      throw new IllegalArgumentException("Legal name is required");
    }
    if (address != null && address.length() > 500) {
      throw new IllegalArgumentException("Address exceeds the persistence contract");
    }
  }
}
