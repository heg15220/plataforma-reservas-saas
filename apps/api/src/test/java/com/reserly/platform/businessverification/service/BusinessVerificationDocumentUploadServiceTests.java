package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.businessverification.document.BusinessDocumentContentValidator;
import com.reserly.platform.businessverification.document.BusinessDocumentUploadProperties;
import com.reserly.platform.businessverification.document.DocumentEncryptionService;
import com.reserly.platform.businessverification.document.MalwareDetectedException;
import com.reserly.platform.businessverification.document.MalwareScanResult;
import com.reserly.platform.businessverification.document.MalwareScanner;
import com.reserly.platform.businessverification.document.PrivateObjectStorage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessVerificationDocumentUploadServiceTests {

  @Mock private BusinessVerificationDocumentPersistenceService persistenceService;
  @Mock private MalwareScanner malwareScanner;
  @Mock private DocumentEncryptionService encryptionService;
  @Mock private PrivateObjectStorage objectStorage;

  private BusinessVerificationDocumentUploadService service;

  @BeforeEach
  void configureService() {
    service =
        new BusinessVerificationDocumentUploadServiceImpl(
            persistenceService,
            new BusinessDocumentContentValidator(new BusinessDocumentUploadProperties(1024)),
            malwareScanner,
            encryptionService,
            objectStorage);
  }

  @Test
  void scansEncryptsStoresAndPersistsOnlyInternalMetadata() {
    byte[] plaintext = "%PDF-1.7\nprivate".getBytes(StandardCharsets.US_ASCII);
    byte[] ciphertext = {1, 2, 3};
    UUID documentId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    Instant uploadedAt = Instant.parse("2026-06-29T09:00:00Z");
    when(malwareScanner.scan(plaintext)).thenReturn(MalwareScanResult.CLEAN);
    when(encryptionService.encrypt(plaintext)).thenReturn(ciphertext);
    when(encryptionService.keyId()).thenReturn("test-v1");
    when(persistenceService.persist(any()))
        .thenReturn(
            new BusinessVerificationDocumentUploadOutcome(
                documentId, requestId, "pending_review", uploadedAt));

    BusinessVerificationDocumentUploadOutcome outcome =
        service.upload(command(requestId, plaintext));

    assertThat(outcome.documentId()).isEqualTo(documentId);
    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    verify(objectStorage).put(key.capture(), org.mockito.ArgumentMatchers.eq(ciphertext));
    assertThat(key.getValue())
        .startsWith("business-verification/")
        .endsWith(".rsy")
        .doesNotContain("http");
    ArgumentCaptor<BusinessVerificationDocumentUploadPersistenceCommand> metadata =
        ArgumentCaptor.forClass(BusinessVerificationDocumentUploadPersistenceCommand.class);
    verify(persistenceService).persist(metadata.capture());
    assertThat(metadata.getValue().objectKey()).isEqualTo(key.getValue());
    assertThat(metadata.getValue().mediaType()).isEqualTo("application/pdf");
    assertThat(metadata.getValue().fileSizeBytes()).isEqualTo(plaintext.length);
    assertThat(metadata.getValue().encryptionKeyId()).isEqualTo("test-v1");
  }

  @Test
  void rejectsMalwareBeforeEncryptionOrStorage() {
    byte[] plaintext = "%PDF-malware".getBytes(StandardCharsets.US_ASCII);
    when(malwareScanner.scan(plaintext)).thenReturn(MalwareScanResult.INFECTED);

    assertThatThrownBy(() -> service.upload(command(UUID.randomUUID(), plaintext)))
        .isInstanceOf(MalwareDetectedException.class);

    verify(encryptionService, never()).encrypt(any());
    verify(objectStorage, never()).put(anyString(), any());
    verify(persistenceService, never()).persist(any());
  }

  @Test
  void deletesStoredObjectWhenMetadataTransactionFails() {
    byte[] plaintext = "%PDF-duplicate".getBytes(StandardCharsets.US_ASCII);
    when(malwareScanner.scan(plaintext)).thenReturn(MalwareScanResult.CLEAN);
    when(encryptionService.encrypt(plaintext)).thenReturn(new byte[] {4, 5, 6});
    when(persistenceService.persist(any())).thenThrow(new IllegalStateException("duplicate"));

    assertThatThrownBy(() -> service.upload(command(UUID.randomUUID(), plaintext)))
        .isInstanceOf(IllegalStateException.class);

    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    verify(objectStorage).delete(key.capture());
    assertThat(key.getValue()).startsWith("business-verification/");
  }

  private BusinessVerificationDocumentUploadCommand command(UUID requestId, byte[] content) {
    return new BusinessVerificationDocumentUploadCommand(
        UUID.randomUUID(),
        requestId,
        UUID.randomUUID(),
        "census_certificate",
        "application/pdf",
        new ByteArrayInputStream(content));
  }
}
