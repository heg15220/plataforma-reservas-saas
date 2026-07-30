package com.reserly.platform.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.dto.AdminBusinessDecisionRequest;
import com.reserly.platform.administration.dto.AdminDocumentReviewRequest;
import com.reserly.platform.administration.dto.AdminPenaltyUpdateRequest;
import com.reserly.platform.businessverification.document.DocumentEncryptionService;
import com.reserly.platform.businessverification.document.PrivateObjectStorage;
import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckDao;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationDocumentRequestEntity;
import com.reserly.platform.businessverification.service.RemoteBusinessVerificationService;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import com.reserly.platform.incidents.persistence.PenaltyEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Cubre las transiciones administrativas de las tareas 14.7, 14.8 y 14.9. */
class AdminDecisionServicesTests {
  private static final Instant NOW = Instant.parse("2026-07-30T20:00:00Z");
  private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final AdminRequestContext CONTEXT =
      new AdminRequestContext("127.0.0.1", "test-agent");

  @Test
  void approvesPendingBusinessAccountAndAuditsDecision() {
    BusinessAccountDao accountDao = mock(BusinessAccountDao.class);
    UserDao userDao = mock(UserDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    UserEntity reviewer = user(ACTOR_ID, "admin@example.com");
    BusinessAccountEntity account = pendingAccount();
    when(accountDao.findByIdForStateUpdate(account.getId())).thenReturn(Optional.of(account));
    when(userDao.findById(ACTOR_ID)).thenReturn(Optional.of(reviewer));
    var service =
        new AdminBusinessAccountServiceImpl(
            accountDao,
            userDao,
            mock(BusinessVerificationCheckDao.class),
            mock(RemoteBusinessVerificationService.class),
            audit,
            fixedClock());

    var response =
        service.decide(
            ACTOR_ID,
            account.getId(),
            new AdminBusinessDecisionRequest("approved", "Documentación coherente"),
            CONTEXT);

    assertThat(response.manualReviewStatus()).isEqualTo("approved");
    assertThat(account.getBusinessVerificationStatus()).isEqualTo("pending_review");
    assertThat(account.getManualReviewedAt()).isEqualTo(NOW);
    verify(accountDao).saveAndFlush(account);
    verify(audit).record(any());
  }

  @Test
  void requestsDocumentCorrectionAndReopensOriginalRequest() {
    BusinessVerificationDocumentDao documentDao = mock(BusinessVerificationDocumentDao.class);
    UserDao userDao = mock(UserDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    BusinessAccountEntity account = pendingAccount();
    BusinessVerificationDocumentRequestEntity documentRequest =
        new BusinessVerificationDocumentRequestEntity();
    documentRequest.setId(UUID.randomUUID());
    documentRequest.setStatus("fulfilled");
    documentRequest.setResolvedAt(NOW.minusSeconds(30));
    BusinessVerificationDocumentEntity document = new BusinessVerificationDocumentEntity();
    document.setId(UUID.randomUUID());
    document.setBusinessAccount(account);
    document.setDocumentRequest(documentRequest);
    document.setDocumentType("census_certificate");
    document.setStatus("pending_review");
    document.setCreatedAt(NOW.minusSeconds(60));
    when(documentDao.findByIdForAdminReview(document.getId()))
        .thenReturn(Optional.of(document));
    when(userDao.findById(ACTOR_ID)).thenReturn(Optional.of(user(ACTOR_ID, "admin@example.com")));
    var service =
        new AdminDocumentServiceImpl(
            documentDao,
            userDao,
            audit,
            fixedClock(),
            mock(PrivateObjectStorage.class),
            mock(DocumentEncryptionService.class));

    service.review(
        ACTOR_ID,
        document.getId(),
        new AdminDocumentReviewRequest("needs_correction", "Copia incompleta"),
        CONTEXT);

    assertThat(document.getStatus()).isEqualTo("needs_correction");
    assertThat(documentRequest.getStatus()).isEqualTo("open");
    assertThat(documentRequest.getResolvedAt()).isNull();
    assertThat(account.getManualReviewStatus()).isEqualTo("needs_correction");
    verify(audit).record(any());
  }

  @Test
  void revokesActivePenaltyWithoutChangingItsEvidence() {
    PenaltyDao penaltyDao = mock(PenaltyDao.class);
    AuditLogService audit = mock(AuditLogService.class);
    PenaltyEntity penalty = new PenaltyEntity();
    penalty.setId(UUID.randomUUID());
    penalty.setCustomerEmailNormalized("client@example.com");
    penalty.setScope("global");
    penalty.setIncidentCountOperational(2);
    penalty.setStartsAt(NOW.minusSeconds(3600));
    penalty.setEndsAt(NOW.plusSeconds(3600));
    penalty.setStatus("active");
    penalty.setReason("operational_no_show_incidents");
    penalty.setCreatedFromIncidentId(UUID.randomUUID());
    penalty.setUpdatedAt(NOW.minusSeconds(60));
    when(penaltyDao.findByIdForAdminUpdate(penalty.getId())).thenReturn(Optional.of(penalty));
    var service = new AdminPenaltyServiceImpl(penaltyDao, audit, fixedClock());

    var response =
        service.update(
            ACTOR_ID,
            penalty.getId(),
            new AdminPenaltyUpdateRequest("revoked", null, "Reclamación aceptada"),
            CONTEXT);

    assertThat(response.status()).isEqualTo("revoked");
    assertThat(response.createdFromIncidentId()).isEqualTo(penalty.getCreatedFromIncidentId());
    verify(penaltyDao).saveAndFlush(penalty);
    verify(audit).record(any());
  }

  private BusinessAccountEntity pendingAccount() {
    BusinessAccountEntity account = new BusinessAccountEntity();
    account.setId(UUID.randomUUID());
    account.setOwnerUser(user(UUID.randomUUID(), "owner@example.com"));
    account.setTaxCountry("ES");
    account.setBusinessLegalName("Empresa SL");
    account.setBusinessTaxIdentifier("B12345678");
    account.setBusinessVerificationStatus("pending_review");
    account.setManualReviewStatus("pending_review");
    account.setUpdatedAt(NOW.minusSeconds(60));
    return account;
  }

  private UserEntity user(UUID id, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    return user;
  }

  private Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }
}
