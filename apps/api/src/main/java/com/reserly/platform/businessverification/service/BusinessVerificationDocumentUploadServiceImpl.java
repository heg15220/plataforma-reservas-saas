package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.document.BusinessDocumentContentValidator;
import com.reserly.platform.businessverification.document.DocumentEncryptionService;
import com.reserly.platform.businessverification.document.MalwareDetectedException;
import com.reserly.platform.businessverification.document.MalwareScanResult;
import com.reserly.platform.businessverification.document.MalwareScanner;
import com.reserly.platform.businessverification.document.PrivateObjectStorage;
import com.reserly.platform.businessverification.document.ValidatedBusinessDocumentContent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Pipeline fail-closed: autorización, validación, antivirus, cifrado, objeto y metadatos.
 *
 * <p>No mantiene una transacción durante lectura, escaneo, cifrado o S3. Si falla la persistencia,
 * intenta eliminar el objeto único como compensación.
 */
@Service
public class BusinessVerificationDocumentUploadServiceImpl
    implements BusinessVerificationDocumentUploadService {

  private final BusinessVerificationDocumentPersistenceService persistenceService;
  private final BusinessDocumentContentValidator contentValidator;
  private final MalwareScanner malwareScanner;
  private final DocumentEncryptionService encryptionService;
  private final PrivateObjectStorage objectStorage;

  public BusinessVerificationDocumentUploadServiceImpl(
      BusinessVerificationDocumentPersistenceService persistenceService,
      BusinessDocumentContentValidator contentValidator,
      MalwareScanner malwareScanner,
      DocumentEncryptionService encryptionService,
      PrivateObjectStorage objectStorage) {
    this.persistenceService = persistenceService;
    this.contentValidator = contentValidator;
    this.malwareScanner = malwareScanner;
    this.encryptionService = encryptionService;
    this.objectStorage = objectStorage;
  }

  @Override
  public BusinessVerificationDocumentUploadOutcome upload(
      BusinessVerificationDocumentUploadCommand command) {
    String documentType = canonicalDocumentType(command.documentType());
    persistenceService.validateUploadAuthorization(
        command.businessAccountId(),
        command.documentRequestId(),
        command.uploaderUserId(),
        documentType);
    ValidatedBusinessDocumentContent content =
        contentValidator.validate(command.declaredMediaType(), command.content());
    Instant scannedAt = Instant.now();
    if (malwareScanner.scan(content.bytes()) != MalwareScanResult.CLEAN) {
      throw new MalwareDetectedException();
    }
    byte[] encrypted = encryptionService.encrypt(content.bytes());
    String objectKey =
        "business-verification/" + command.businessAccountId() + "/" + UUID.randomUUID() + ".rsy";
    objectStorage.put(objectKey, encrypted);

    try {
      return persistenceService.persist(
          new BusinessVerificationDocumentUploadPersistenceCommand(
              command.businessAccountId(),
              command.documentRequestId(),
              command.uploaderUserId(),
              documentType,
              objectKey,
              content.sha256(),
              content.mediaType(),
              content.bytes().length,
              scannedAt,
              encryptionService.keyId()));
    } catch (RuntimeException exception) {
      try {
        objectStorage.delete(objectKey);
      } catch (RuntimeException compensationFailure) {
        exception.addSuppressed(compensationFailure);
      }
      throw exception;
    }
  }

  private String canonicalDocumentType(String documentType) {
    try {
      return BusinessVerificationDocumentType.valueOf(
              documentType.strip().toUpperCase(java.util.Locale.ROOT))
          .persistedValue();
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new com.reserly.platform.businessverification.document
          .BusinessDocumentUploadValidationException();
    }
  }
}
