package com.reserly.platform.businessverification.remote;

import java.time.Instant;
import java.util.Objects;

/**
 * Resultado remoto minimizado y seguro para auditoría.
 *
 * @param status resultado técnico, independiente del estado publicable de la cuenta
 * @param matchedLegalName coincidencia opcional informada o calculada por el adaptador
 * @param matchedAddress coincidencia opcional de dirección
 * @param remoteReference referencia opaca del proveedor
 * @param checkedAt instante UTC de la comprobación
 * @param rawResponseHash SHA-256 opcional del cuerpo canónico cuando se requiera integridad
 */
public record RemoteBusinessVerificationResult(
    RemoteVerificationStatus status,
    Boolean matchedLegalName,
    Boolean matchedAddress,
    String remoteReference,
    Instant checkedAt,
    String rawResponseHash) {

  public RemoteBusinessVerificationResult {
    Objects.requireNonNull(status);
    Objects.requireNonNull(checkedAt);
    if (remoteReference != null && remoteReference.length() > 255) {
      throw new IllegalArgumentException("Remote reference exceeds the audit contract");
    }
    if (rawResponseHash != null && !rawResponseHash.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Response hash must be SHA-256 hexadecimal");
    }
  }
}
