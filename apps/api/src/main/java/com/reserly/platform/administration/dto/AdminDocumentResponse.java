package com.reserly.platform.administration.dto;

import java.time.Instant;
import java.util.UUID;

/** Metadatos privados mínimos de un documento sometido a revisión. */
public record AdminDocumentResponse(
    UUID id,
    UUID businessAccountId,
    UUID documentRequestId,
    String documentType,
    String mediaType,
    Long fileSizeBytes,
    String malwareScanStatus,
    String status,
    Instant createdAt,
    Instant reviewedAt,
    String reviewNotes) {}
