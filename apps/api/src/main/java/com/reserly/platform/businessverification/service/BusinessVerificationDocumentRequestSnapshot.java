package com.reserly.platform.businessverification.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Contrato interno de un requerimiento documental.
 *
 * @param requestId identificador opaco
 * @param businessAccountId cuenta afectada
 * @param sourceVerificationCheckId check que originó el requerimiento
 * @param reasonCode motivo controlado
 * @param reasonMessageKey clave i18n futura
 * @param requestedDocumentTypes tipos que puede aportar el titular
 * @param status estado del requerimiento
 * @param requestedAt instante de creación
 */
public record BusinessVerificationDocumentRequestSnapshot(
    UUID requestId,
    UUID businessAccountId,
    UUID sourceVerificationCheckId,
    String reasonCode,
    String reasonMessageKey,
    List<String> requestedDocumentTypes,
    String status,
    Instant requestedAt) {

  public BusinessVerificationDocumentRequestSnapshot {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(businessAccountId);
    Objects.requireNonNull(sourceVerificationCheckId);
    Objects.requireNonNull(reasonCode);
    Objects.requireNonNull(reasonMessageKey);
    requestedDocumentTypes = List.copyOf(requestedDocumentTypes);
    Objects.requireNonNull(status);
    Objects.requireNonNull(requestedAt);
  }
}
