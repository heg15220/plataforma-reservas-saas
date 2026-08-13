package com.reserly.platform.demand.attribute.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeCandidateDao;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeCandidateEntity;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeDao;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Protege la revisión humana, las fusiones trazables y la minimización de propuestas. */
class DemandAttributeGovernanceServiceTests {
  private static final UUID ACTOR = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

  @Test
  void rejectsDirectIdentifiersInCandidateExamples() {
    Fixture fixture = fixture();
    DemandAttributeCandidateRequest request =
        new DemandAttributeCandidateRequest(
            "quietLighting",
            "cluster-1",
            "ambience",
            "subjectiveAggregate",
            "Luz serena",
            "Calm lighting",
            "Definición",
            "Definition",
            List.of("customerAggregate"),
            List.of("Comentario de name@example.invalid"));

    assertThatThrownBy(() -> fixture.service.createCandidate(ACTOR, request, context()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("minimizados");
  }

  @Test
  void publishesReviewedCandidateAndAuditsBothEntities() {
    Fixture fixture = fixture();
    UUID candidateId = UUID.randomUUID();
    DemandAttributeCandidateEntity candidate = candidate(candidateId, "in_review");
    when(fixture.candidateDao.findById(candidateId)).thenReturn(Optional.of(candidate));
    when(fixture.attributeDao.findByCode(candidate.getProposedCode())).thenReturn(Optional.empty());
    when(fixture.attributeDao.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(fixture.candidateDao.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DemandAttributeCandidateAdminResponse result =
        fixture.service.transitionCandidate(
            ACTOR,
            candidateId,
            new DemandAttributeTransitionRequest("published", null, null),
            context());

    assertThat(result.governanceStatus()).isEqualTo("published");
    assertThat(candidate.getPublishedAt()).isEqualTo(NOW);
    verify(fixture.attributeDao).saveAndFlush(any(DemandAttributeEntity.class));
    verify(fixture.auditLogService, org.mockito.Mockito.times(2)).record(any());
  }

  private Fixture fixture() {
    DemandAttributeDao attributeDao = mock(DemandAttributeDao.class);
    DemandAttributeCandidateDao candidateDao = mock(DemandAttributeCandidateDao.class);
    AuditLogService auditLogService = mock(AuditLogService.class);
    return new Fixture(
        attributeDao,
        candidateDao,
        auditLogService,
        new DemandAttributeGovernanceService(
            attributeDao, candidateDao, auditLogService, Clock.fixed(NOW, ZoneOffset.UTC)));
  }

  private DemandAttributeCandidateEntity candidate(UUID id, String status) {
    DemandAttributeCandidateEntity candidate = new DemandAttributeCandidateEntity();
    candidate.setId(id);
    candidate.setProposedCode("quietLighting");
    candidate.setClusterKey("cluster-1");
    candidate.setFamily("ambience");
    candidate.setAttributeType("subjectiveAggregate");
    candidate.setNameEs("Luz serena");
    candidate.setNameEn("Calm lighting");
    candidate.setDefinitionEs("Definición");
    candidate.setDefinitionEn("Definition");
    candidate.setAllowedSources(List.of("customerAggregate"));
    candidate.setExampleSummaries(List.of("Síntesis agregada"));
    candidate.setGovernanceStatus(status);
    candidate.setCreatedAt(NOW.minusSeconds(60));
    candidate.setUpdatedAt(NOW.minusSeconds(60));
    return candidate;
  }

  private AdminRequestContext context() {
    return new AdminRequestContext("127.0.0.1", "test");
  }

  private record Fixture(
      DemandAttributeDao attributeDao,
      DemandAttributeCandidateDao candidateDao,
      AuditLogService auditLogService,
      DemandAttributeGovernanceService service) {}
}
