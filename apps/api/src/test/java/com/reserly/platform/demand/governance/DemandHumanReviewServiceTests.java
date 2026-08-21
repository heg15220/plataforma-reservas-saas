package com.reserly.platform.demand.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.venues.persistence.VenueDao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

/** Cubre bloqueo previo, revisión, corrección, impugnación y minimización del workflow. */
class DemandHumanReviewServiceTests {
  private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");
  private final DemandHumanReviewDao dao = mock(DemandHumanReviewDao.class);
  private final VenueDao venueDao = mock(VenueDao.class);
  private final AuditLogService audit = mock(AuditLogService.class);
  private final DemandHumanReviewService service =
      new DemandHumanReviewService(dao, venueDao, audit, Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void persistSameEntity() {
    when(dao.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void submissionIsBlockedUntilHumanApprovalAndAuditedWithoutPayload() {
    DemandHumanReviewSubmissionRequest request = request("attribute", null);

    DemandHumanReviewResponse response = service.submit("prefect-worker-v1", request);

    assertThat(response.status()).isEqualTo("submitted");
    assertThat(response.executionAuthorized()).isFalse();
    verify(dao).lockSubmission(request.reviewId());
    ArgumentCaptor<AuditLogEntry> entry = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actorRole()).isEqualTo("system");
    assertThat(entry.getValue().afterJson())
        .containsKeys("subjectKey", "subjectVersion", "policyVersion", "explanationCode")
        .doesNotContainKeys("evidenceSha256", "payload", "email");
  }

  @Test
  void identicalSubmissionReplaysButDivergentContentConflicts() {
    DemandHumanReviewSubmissionRequest request = request("attribute", null);
    DemandHumanReviewEntity existing = entity(request, "submitted");
    when(dao.findById(request.reviewId())).thenReturn(Optional.of(existing));

    assertThat(service.submit("prefect-worker-v1", request).id()).isEqualTo(request.reviewId());
    verify(dao, never()).saveAndFlush(any());

    DemandHumanReviewSubmissionRequest divergent =
        new DemandHumanReviewSubmissionRequest(
            request.reviewId(),
            request.reviewType(),
            request.subjectType(),
            "other:key-v1",
            request.subjectVersion(),
            request.venueId(),
            request.policyVersion(),
            request.explanationCode(),
            request.evidenceSha256());
    assertThatThrownBy(() -> service.submit("prefect-worker-v1", divergent))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");
  }

  @Test
  void adminApprovalIsOnlyExecutionAuthorization() {
    DemandHumanReviewSubmissionRequest request = request("commercial_decision", UUID.randomUUID());
    DemandHumanReviewEntity entity = entity(request, "submitted");
    when(dao.findByIdForUpdate(request.reviewId())).thenReturn(Optional.of(entity));

    DemandHumanReviewResponse response =
        service.decide(
            UUID.randomUUID(),
            request.reviewId(),
            new DemandHumanReviewDecisionRequest("approved", "risk_review_passed", null),
            new AdminRequestContext("127.0.0.1", "test"));

    assertThat(response.status()).isEqualTo("approved");
    assertThat(response.executionAuthorized()).isTrue();
  }

  @Test
  void correctionRequiresNewVersionAndCannotSkipState() {
    DemandHumanReviewSubmissionRequest request = request("attribute", null);
    DemandHumanReviewEntity entity = entity(request, "submitted");
    when(dao.findByIdForUpdate(request.reviewId())).thenReturn(Optional.of(entity));

    assertThatThrownBy(
            () ->
                service.decide(
                    UUID.randomUUID(),
                    request.reviewId(),
                    new DemandHumanReviewDecisionRequest("corrected", "fixed", null),
                    new AdminRequestContext(null, null)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void accessibleVenueCanAppealExactlyOnceAndAuthorizationIsRevoked() {
    UUID actor = UUID.randomUUID();
    UUID venue = UUID.randomUUID();
    DemandHumanReviewSubmissionRequest request = request("commercial_decision", venue);
    DemandHumanReviewEntity entity = entity(request, "approved");
    when(dao.findByIdForUpdate(request.reviewId())).thenReturn(Optional.of(entity));
    when(venueDao.findAccessibleById(actor, venue))
        .thenReturn(Optional.of(mock(com.reserly.platform.venues.persistence.VenueEntity.class)));

    DemandHumanReviewResponse response =
        service.appeal(
            actor,
            request.reviewId(),
            new DemandHumanReviewAppealRequest("incorrect_inputs"),
            new AdminRequestContext(null, null));

    assertThat(response.status()).isEqualTo("appealed");
    assertThat(response.executionAuthorized()).isFalse();
    assertThatThrownBy(
            () ->
                service.appeal(
                    actor,
                    request.reviewId(),
                    new DemandHumanReviewAppealRequest("incorrect_inputs"),
                    new AdminRequestContext(null, null)))
        .isInstanceOf(ResponseStatusException.class);
  }

  private DemandHumanReviewSubmissionRequest request(String type, UUID venueId) {
    return new DemandHumanReviewSubmissionRequest(
        UUID.randomUUID(),
        type,
        "attribute".equals(type) ? "attribute_candidate" : "commercial_action",
        "demand:subject-v1",
        "subject-v1",
        venueId,
        "human-review-v1",
        "material_impact",
        "a".repeat(64));
  }

  private DemandHumanReviewEntity entity(
      DemandHumanReviewSubmissionRequest request, String status) {
    DemandHumanReviewEntity entity = new DemandHumanReviewEntity();
    entity.setId(request.reviewId());
    entity.setReviewType(request.reviewType());
    entity.setSubjectType(request.subjectType());
    entity.setSubjectKey(request.subjectKey());
    entity.setSubjectVersion(request.subjectVersion());
    entity.setVenueId(request.venueId());
    entity.setPolicyVersion(request.policyVersion());
    entity.setExplanationCode(request.explanationCode());
    entity.setEvidenceSha256(request.evidenceSha256());
    entity.setStatus(status);
    entity.setRequestedByService("prefect-worker-v1");
    entity.setSubmittedAt(NOW);
    entity.setUpdatedAt(NOW);
    return entity;
  }
}
