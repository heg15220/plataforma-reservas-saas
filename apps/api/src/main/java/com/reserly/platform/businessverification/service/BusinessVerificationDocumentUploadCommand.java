package com.reserly.platform.businessverification.service;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Comando interno de carga; el actor deberá proceder de autenticación cuando exista endpoint.
 *
 * @param businessAccountId cuenta destino
 * @param documentRequestId requerimiento abierto
 * @param uploaderUserId actor autenticado
 * @param documentType tipo solicitado
 * @param declaredMediaType MIME declarado
 * @param content stream acotado y cerrado por el servicio
 */
public record BusinessVerificationDocumentUploadCommand(
    UUID businessAccountId,
    UUID documentRequestId,
    UUID uploaderUserId,
    String documentType,
    String declaredMediaType,
    InputStream content) {

  public BusinessVerificationDocumentUploadCommand {
    Objects.requireNonNull(businessAccountId);
    Objects.requireNonNull(documentRequestId);
    Objects.requireNonNull(uploaderUserId);
    Objects.requireNonNull(documentType);
    Objects.requireNonNull(declaredMediaType);
    Objects.requireNonNull(content);
  }
}
