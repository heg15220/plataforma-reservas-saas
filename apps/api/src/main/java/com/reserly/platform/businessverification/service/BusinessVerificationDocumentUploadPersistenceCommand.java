package com.reserly.platform.businessverification.service;

import java.time.Instant;
import java.util.UUID;

/** Metadatos seguros que se persisten después de almacenar el objeto cifrado. */
public record BusinessVerificationDocumentUploadPersistenceCommand(
    UUID businessAccountId,
    UUID documentRequestId,
    UUID uploaderUserId,
    String documentType,
    String objectKey,
    String fileHash,
    String mediaType,
    long fileSizeBytes,
    Instant malwareScannedAt,
    String encryptionKeyId) {}
