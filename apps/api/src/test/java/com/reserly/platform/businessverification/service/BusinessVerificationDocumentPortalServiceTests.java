package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Verifica derivación de ownership y delegación al pipeline documental privado. */
@ExtendWith(MockitoExtension.class)
class BusinessVerificationDocumentPortalServiceTests {

  @Mock private BusinessAccountDao businessAccountDao;
  @Mock private BusinessVerificationDocumentRequestService documentRequestService;
  @Mock private BusinessVerificationDocumentUploadService documentUploadService;

  private BusinessVerificationDocumentPortalService service;

  @BeforeEach
  void setUp() {
    service =
        new BusinessVerificationDocumentPortalServiceImpl(
            businessAccountDao, documentRequestService, documentUploadService);
  }

  @Test
  void findsOnlyTheOpenRequestOwnedByTheAuthenticatedUser() {
    UUID userId = UUID.randomUUID();
    BusinessAccountEntity account = account();
    BusinessVerificationDocumentRequestSnapshot snapshot = snapshot(account.getId());
    when(businessAccountDao.findByOwnerUserId(userId)).thenReturn(Optional.of(account));
    when(documentRequestService.findOpen(account.getId())).thenReturn(Optional.of(snapshot));

    assertThat(service.findOpenRequest(userId)).contains(snapshot);
    verify(documentRequestService).findOpen(account.getId());
  }

  @Test
  void returnsEmptyWhenTheAuthenticatedUserHasNoBusinessAccount() {
    UUID userId = UUID.randomUUID();
    when(businessAccountDao.findByOwnerUserId(userId)).thenReturn(Optional.empty());

    assertThat(service.findOpenRequest(userId)).isEmpty();
  }

  @Test
  void derivesBusinessAccountAndUploaderFromTheAuthenticatedUser() {
    UUID userId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    BusinessAccountEntity account = account();
    byte[] bytes = "%PDF-document".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    BusinessVerificationDocumentUploadOutcome outcome =
        new BusinessVerificationDocumentUploadOutcome(
            UUID.randomUUID(), requestId, "pending_review", Instant.now());
    when(businessAccountDao.findByOwnerUserId(userId)).thenReturn(Optional.of(account));
    when(documentUploadService.upload(any())).thenReturn(outcome);

    assertThat(
            service.upload(
                userId,
                requestId,
                "census_certificate",
                "application/pdf",
                new ByteArrayInputStream(bytes)))
        .isEqualTo(outcome);

    ArgumentCaptor<BusinessVerificationDocumentUploadCommand> captor =
        ArgumentCaptor.forClass(BusinessVerificationDocumentUploadCommand.class);
    verify(documentUploadService).upload(captor.capture());
    assertThat(captor.getValue().businessAccountId()).isEqualTo(account.getId());
    assertThat(captor.getValue().uploaderUserId()).isEqualTo(userId);
    assertThat(captor.getValue().documentRequestId()).isEqualTo(requestId);
  }

  @Test
  void hidesMissingOwnershipAndPersistentConflictsBehindDomainErrors() {
    UUID userId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    when(businessAccountDao.findByOwnerUserId(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.upload(
                    userId,
                    requestId,
                    "other",
                    "application/pdf",
                    new ByteArrayInputStream(new byte[] {1})))
        .isInstanceOf(BusinessVerificationDocumentUploadForbiddenException.class);

    BusinessAccountEntity account = account();
    when(businessAccountDao.findByOwnerUserId(userId)).thenReturn(Optional.of(account));
    when(documentUploadService.upload(any()))
        .thenThrow(new DataIntegrityViolationException("private constraint"));

    assertThatThrownBy(
            () ->
                service.upload(
                    userId,
                    requestId,
                    "other",
                    "application/pdf",
                    new ByteArrayInputStream(new byte[] {1})))
        .isInstanceOf(BusinessVerificationDocumentUploadConflictException.class)
        .hasCauseInstanceOf(DataIntegrityViolationException.class);
  }

  private BusinessAccountEntity account() {
    BusinessAccountEntity account = new BusinessAccountEntity();
    account.setId(UUID.randomUUID());
    return account;
  }

  private BusinessVerificationDocumentRequestSnapshot snapshot(UUID accountId) {
    return new BusinessVerificationDocumentRequestSnapshot(
        UUID.randomUUID(),
        accountId,
        UUID.randomUUID(),
        "no_automated_channel",
        "businessVerification.documents.reason.no_automated_channel",
        List.of("census_certificate"),
        "open",
        Instant.parse("2026-07-01T08:00:00Z"));
  }
}
