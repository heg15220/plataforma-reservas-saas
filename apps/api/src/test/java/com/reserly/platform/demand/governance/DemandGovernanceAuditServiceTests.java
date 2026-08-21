package com.reserly.platform.demand.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.persistence.AuditLogDao;
import com.reserly.platform.administration.persistence.AuditLogEntity;
import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Cubre las siete familias, minimización, idempotencia y validación fail-closed. */
class DemandGovernanceAuditServiceTests {
  private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");
  private static final String DIGEST = "a".repeat(64);
  private final AuditLogDao dao = mock(AuditLogDao.class);
  private final AuditLogService auditLogService = mock(AuditLogService.class);
  private final DemandGovernanceAuditService service =
      new DemandGovernanceAuditService(dao, auditLogService);

  @BeforeEach
  void saveReturnsPersistedEntity() {
    when(auditLogService.record(any()))
        .thenAnswer(invocation -> persisted(invocation.getArgument(0)));
  }

  @Test
  void recordsEveryGovernedFamilyWithClosedActionAndMinimizedSnapshot() {
    List<DemandGovernanceAuditRequest> requests =
        List.of(
            request("ontology", "approved", false, DIGEST),
            request("ranking_weights", "activated", false, DIGEST),
            request("model", "registered", false, DIGEST),
            request("experiment", "started", false, DIGEST),
            request("promotion", "executed", false, DIGEST),
            request("waitlist", "allocated", true, null),
            request("automatic_action", "rolled_back", true, null));

    requests.forEach(request -> service.recordSystem("prefect-worker-v1", request));

    ArgumentCaptor<AuditLogEntry> entries = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(auditLogService, org.mockito.Mockito.times(7)).record(entries.capture());
    requests.forEach(request -> verify(dao).lockDemandGovernanceEvent(request.eventId()));
    assertThat(entries.getAllValues())
        .extracting(AuditLogEntry::entityType)
        .containsExactly(
            "demand_ontology",
            "demand_ranking_weights",
            "demand_model",
            "demand_experiment",
            "demand_promotion",
            "demand_waitlist",
            "demand_automatic_action");
    assertThat(entries.getAllValues())
        .allSatisfy(
            entry -> {
              assertThat(entry.actorRole()).isEqualTo("system");
              assertThat(entry.actorUserId()).isNull();
              assertThat(entry.ipAddress()).isNull();
              assertThat(entry.userAgent()).isNull();
              assertThat(entry.afterJson())
                  .containsKeys(
                      "resourceKey",
                      "version",
                      "policyVersion",
                      "reasonCode",
                      "effectiveFrom",
                      "correlationId",
                      "sourceServiceId",
                      "automated")
                  .doesNotContainKeys("payload", "email", "customerId", "metrics");
            });
  }

  @Test
  void identicalReplayReturnsExistingEntryWithoutAppending() {
    DemandGovernanceAuditRequest request = request("model", "registered", false, DIGEST);
    AuditLogEntry expected = entryFor(request);
    AuditLogEntity existing = persisted(expected);
    when(dao.findByEntityTypeAndEntityId("demand_model", request.eventId()))
        .thenReturn(Optional.of(existing));

    DemandGovernanceAuditResponse response = service.recordSystem("prefect-worker-v1", request);

    assertThat(response.auditLogId()).isEqualTo(existing.getId());
    verify(auditLogService, never()).record(any());
  }

  @Test
  void eventIdReplayWithDifferentContentFailsClosed() {
    DemandGovernanceAuditRequest request = request("model", "registered", false, DIGEST);
    AuditLogEntity existing = persisted(entryFor(request));
    existing.setAction("governance.retired");
    when(dao.findByEntityTypeAndEntityId("demand_model", request.eventId()))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.recordSystem("prefect-worker-v1", request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("DEMAND_GOVERNANCE_EVENT_ID_CONFLICT");
  }

  @Test
  void artifactApprovalAutomationAndPeriodRulesFailClosed() {
    DemandGovernanceAuditRequest modelWithoutDigest = request("model", "registered", false, null);
    DemandGovernanceAuditRequest humanWithoutApproval =
        copy(request("waitlist", "accepted", false, null), null, false, NOW.plusSeconds(60));
    DemandGovernanceAuditRequest automaticWithApproval =
        copy(
            request("automatic_action", "triggered", true, null),
            "approval-v1",
            true,
            NOW.plusSeconds(60));
    DemandGovernanceAuditRequest invalidPeriod =
        copy(request("waitlist", "allocated", true, null), null, true, NOW.minusSeconds(1));
    DemandGovernanceAuditRequest automaticPromotion =
        copy(request("promotion", "executed", false, DIGEST), null, true, NOW.plusSeconds(60));

    for (DemandGovernanceAuditRequest invalid :
        List.of(
            modelWithoutDigest,
            humanWithoutApproval,
            automaticWithApproval,
            invalidPeriod,
            automaticPromotion)) {
      assertThatThrownBy(() -> service.recordSystem("prefect-worker-v1", invalid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("DEMAND_GOVERNANCE_AUDIT_INVALID");
    }
    verify(auditLogService, never()).record(any());
  }

  @Test
  void unknownFamilyActionAndFreeReasonAreRejected() {
    DemandGovernanceAuditRequest unknown = request("customer", "updated", true, null);
    DemandGovernanceAuditRequest action = request("model", "deleted", false, DIGEST);
    DemandGovernanceAuditRequest reason = request("waitlist", "allocated", true, null);
    reason =
        new DemandGovernanceAuditRequest(
            reason.eventId(),
            reason.resourceType(),
            reason.resourceKey(),
            reason.action(),
            "Manual reason",
            reason.beforeVersion(),
            reason.afterVersion(),
            reason.policyVersion(),
            reason.artifactSha256(),
            reason.effectiveFrom(),
            reason.effectiveUntil(),
            reason.correlationId(),
            reason.automated(),
            reason.approvalReference());

    for (DemandGovernanceAuditRequest invalid : List.of(unknown, action, reason)) {
      assertThatThrownBy(() -> service.recordSystem("prefect-worker-v1", invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  private DemandGovernanceAuditRequest request(
      String resourceType, String action, boolean automated, String digest) {
    return new DemandGovernanceAuditRequest(
        UUID.randomUUID(),
        resourceType,
        resourceType + ":resource-v1",
        action,
        "scheduled_review",
        "resource-v0",
        "resource-v1",
        "governance-policy-v1",
        digest,
        NOW,
        NOW.plusSeconds(3600),
        UUID.randomUUID(),
        automated,
        automated ? null : "admin-approval-v1");
  }

  private DemandGovernanceAuditRequest copy(
      DemandGovernanceAuditRequest source,
      String approvalReference,
      boolean automated,
      Instant effectiveUntil) {
    return new DemandGovernanceAuditRequest(
        source.eventId(),
        source.resourceType(),
        source.resourceKey(),
        source.action(),
        source.reasonCode(),
        source.beforeVersion(),
        source.afterVersion(),
        source.policyVersion(),
        source.artifactSha256(),
        source.effectiveFrom(),
        effectiveUntil,
        source.correlationId(),
        automated,
        approvalReference);
  }

  private AuditLogEntry entryFor(DemandGovernanceAuditRequest request) {
    service.recordSystem("prefect-worker-v1", request);
    ArgumentCaptor<AuditLogEntry> entry = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(auditLogService).record(entry.capture());
    org.mockito.Mockito.reset(auditLogService);
    when(auditLogService.record(any()))
        .thenAnswer(invocation -> persisted(invocation.getArgument(0)));
    return entry.getValue();
  }

  private AuditLogEntity persisted(AuditLogEntry entry) {
    AuditLogEntity entity = new AuditLogEntity();
    entity.setId(UUID.randomUUID());
    entity.setActorUserId(entry.actorUserId());
    entity.setActorRole(entry.actorRole());
    entity.setEntityType(entry.entityType());
    entity.setEntityId(entry.entityId());
    entity.setAction(entry.action());
    entity.setBeforeJson(entry.beforeJson());
    entity.setAfterJson(entry.afterJson());
    entity.setCreatedAt(NOW);
    return entity;
  }
}
