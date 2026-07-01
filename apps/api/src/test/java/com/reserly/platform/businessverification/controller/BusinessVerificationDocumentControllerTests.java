package com.reserly.platform.businessverification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.businessverification.converter.BusinessVerificationDocumentConverter;
import com.reserly.platform.businessverification.document.BusinessDocumentUploadValidationException;
import com.reserly.platform.businessverification.document.MalwareDetectedException;
import com.reserly.platform.businessverification.document.MalwareScannerUnavailableException;
import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentRequestResponse;
import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentUploadResponse;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentPortalService;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentRequestSnapshot;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentUploadConflictException;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentUploadForbiddenException;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentUploadOutcome;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

/** Prueba la proyección REST sin arrancar almacenamiento, antivirus ni infraestructura. */
@ExtendWith(MockitoExtension.class)
class BusinessVerificationDocumentControllerTests {

  @Mock private BusinessVerificationDocumentPortalService portalService;

  private BusinessVerificationDocumentControllerImpl controller;
  private BusinessVerificationDocumentExceptionHandler exceptionHandler;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller =
        new BusinessVerificationDocumentControllerImpl(
            portalService, new BusinessVerificationDocumentConverter());
    exceptionHandler = new BusinessVerificationDocumentExceptionHandler();
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void returnsOpenRequestWithoutBusinessOrCheckIdentifiers() {
    BusinessVerificationDocumentRequestSnapshot snapshot =
        new BusinessVerificationDocumentRequestSnapshot(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "provider_unavailable",
            "businessVerification.documents.reason.provider_unavailable",
            List.of("census_certificate", "other"),
            "open",
            Instant.parse("2026-07-01T08:00:00Z"));
    when(portalService.findOpenRequest(account.userId())).thenReturn(Optional.of(snapshot));

    ResponseEntity<BusinessVerificationDocumentRequestResponse> response =
        controller.findOpenRequest(account);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().requestId()).isEqualTo(snapshot.requestId());
    assertThat(response.getBody().requestedDocumentTypes())
        .containsExactly("census_certificate", "other");
  }

  @Test
  void returnsNoContentWhenNoDocumentIsRequired() {
    when(portalService.findOpenRequest(account.userId())).thenReturn(Optional.empty());

    assertThat(controller.findOpenRequest(account).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void uploadsMultipartContentUsingOnlyTheAuthenticatedActor() {
    UUID requestId = UUID.randomUUID();
    BusinessVerificationDocumentUploadOutcome outcome =
        new BusinessVerificationDocumentUploadOutcome(
            UUID.randomUUID(), requestId, "pending_review", Instant.parse("2026-07-01T09:00:00Z"));
    when(portalService.upload(
            eq(account.userId()),
            eq(requestId),
            eq("census_certificate"),
            eq("application/pdf"),
            any(InputStream.class)))
        .thenReturn(outcome);
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "sensitive-name.pdf",
            "application/pdf",
            "%PDF-content".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

    ResponseEntity<BusinessVerificationDocumentUploadResponse> response =
        controller.upload(account, requestId, "census_certificate", file);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation())
        .hasToString("/api/venue/me/business-verification/documents/" + outcome.documentId());
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo("pending_review");
    verify(portalService)
        .upload(
            eq(account.userId()),
            eq(requestId),
            eq("census_certificate"),
            eq("application/pdf"),
            any(InputStream.class));
  }

  @Test
  void rejectsEmptyOrUntypedMultipartContentBeforeTheService() {
    MockMultipartFile empty =
        new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    MockMultipartFile untyped = new MockMultipartFile("file", "document.pdf", null, new byte[] {1});

    assertThatThrownBy(() -> controller.upload(account, UUID.randomUUID(), "other", empty))
        .isInstanceOf(BusinessDocumentUploadValidationException.class);
    assertThatThrownBy(() -> controller.upload(account, UUID.randomUUID(), "other", untyped))
        .isInstanceOf(BusinessDocumentUploadValidationException.class);
  }

  @Test
  void mapsExpectedFailuresToStableNonSensitiveCodes() {
    assertThat(exceptionHandler.handleInvalid().getBody().error())
        .isEqualTo("DOCUMENT_UPLOAD_INVALID");
    assertThat(exceptionHandler.handleForbidden().getBody().error())
        .isEqualTo("DOCUMENT_UPLOAD_FORBIDDEN");
    assertThat(exceptionHandler.handleConflict().getBody().error())
        .isEqualTo("DOCUMENT_UPLOAD_CONFLICT");
    assertThat(exceptionHandler.handleMalware().getBody().error())
        .isEqualTo("DOCUMENT_MALWARE_DETECTED");
    assertThat(exceptionHandler.handleUnavailable().getBody().error())
        .isEqualTo("DOCUMENT_UPLOAD_UNAVAILABLE");

    assertThat(exceptionHandler.handleForbidden().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(exceptionHandler.handleConflict().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exceptionHandler.handleMalware().getStatusCode())
        .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    assertThat(exceptionHandler.handleUnavailable().getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void domainExceptionsNeverExposeTheirMessagesInResponses() {
    assertThat(new BusinessVerificationDocumentUploadForbiddenException())
        .hasMessageNotContaining("DOCUMENT_UPLOAD_FORBIDDEN");
    assertThat(new BusinessVerificationDocumentUploadConflictException(new RuntimeException()))
        .hasMessageNotContaining("DOCUMENT_UPLOAD_CONFLICT");
    assertThat(new MalwareDetectedException()).hasMessageNotContaining("DOCUMENT_MALWARE_DETECTED");
    assertThat(new MalwareScannerUnavailableException())
        .hasMessageNotContaining("DOCUMENT_UPLOAD_UNAVAILABLE");
  }
}
