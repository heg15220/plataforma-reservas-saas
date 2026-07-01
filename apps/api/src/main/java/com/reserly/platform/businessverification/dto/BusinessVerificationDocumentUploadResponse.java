package com.reserly.platform.businessverification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Confirmación mínima de una carga privada.
 *
 * @param documentId identificador del metadato, nunca una URL
 * @param documentRequestId requerimiento satisfecho
 * @param status estado inicial de revisión
 * @param uploadedAt instante UTC de persistencia
 */
public record BusinessVerificationDocumentUploadResponse(
    UUID documentId, UUID documentRequestId, String status, Instant uploadedAt) {}
