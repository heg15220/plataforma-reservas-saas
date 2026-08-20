package com.reserly.platform.demand.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentDao;
import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentEntity;
import com.reserly.platform.demand.experiment.persistence.ExperimentDefinitionDao;
import com.reserly.platform.demand.experiment.persistence.ExperimentDefinitionEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Prueba estabilidad, exclusión mutua e invariante temporal de la exposición A/B. */
class ExperimentAssignmentServiceTests {

  private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

  private ExperimentDefinitionDao definitionDao;
  private ExperimentAssignmentDao assignmentDao;
  private RecommendationRequestDao requestDao;
  private ExperimentAssignmentService service;

  @BeforeEach
  void setUp() {
    definitionDao = mock(ExperimentDefinitionDao.class);
    assignmentDao = mock(ExperimentAssignmentDao.class);
    requestDao = mock(RecommendationRequestDao.class);
    service =
        new ExperimentAssignmentServiceImpl(
            definitionDao, assignmentDao, requestDao, Clock.fixed(NOW, ZoneOffset.UTC));
    when(assignmentDao.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void derivesTheSameBucketAndVariantForTheSameVersionedUnit() {
    ExperimentDefinitionEntity definition = definition("rankingPilot", "ranking-summer-2026");
    when(definitionDao.findActive(any(), any(), any(Pageable.class)))
        .thenReturn(List.of(definition));
    when(assignmentDao.findByExperimentDefinitionIdAndAssignmentUnitId(any(), any()))
        .thenReturn(Optional.empty());
    when(assignmentDao.findByExclusionGroupAndExclusionWindowKeyAndAssignmentUnitId(
            any(), any(), any()))
        .thenReturn(Optional.empty());
    UUID unitId = UUID.fromString("9b90eb19-00e6-42cc-a2cd-4062c35832f8");

    ExperimentAssignmentResult first =
        service.assign(
            new ExperimentAssignmentCommand("rankingPilot", unitId, NOW.minusSeconds(2)));
    ExperimentAssignmentResult retry =
        service.assign(
            new ExperimentAssignmentCommand("rankingPilot", unitId, NOW.minusSeconds(1)));

    assertThat(retry.bucket()).isEqualTo(first.bucket()).isBetween(0, 9_999);
    assertThat(retry.variantKey()).isEqualTo(first.variantKey());
    assertThat(retry.policyVersion()).isEqualTo(first.policyVersion());
  }

  @Test
  void returnsThePersistedAssignmentWithoutResampling() {
    ExperimentDefinitionEntity definition = definition("rankingPilot", "ranking-summer-2026");
    ExperimentAssignmentEntity persisted = assignment(definition, "control", "rules.v1");
    when(definitionDao.findActive(any(), any(), any(Pageable.class)))
        .thenReturn(List.of(definition));
    when(assignmentDao.findByExperimentDefinitionIdAndAssignmentUnitId(
            definition.getId(), persisted.getAssignmentUnitId()))
        .thenReturn(Optional.of(persisted));

    ExperimentAssignmentResult result =
        service.assign(
            new ExperimentAssignmentCommand(
                "rankingPilot", persisted.getAssignmentUnitId(), NOW.minusSeconds(1)));

    assertThat(result.variantKey()).isEqualTo("control");
    assertThat(result.bucket()).isEqualTo(8_000);
    verify(assignmentDao, never()).saveAndFlush(any());
  }

  @Test
  void rejectsAnotherExperimentInTheSameExclusionWindow() {
    ExperimentDefinitionEntity definition = definition("rankingPilotB", "ranking-summer-2026");
    UUID unitId = UUID.randomUUID();
    when(definitionDao.findActive(any(), any(), any(Pageable.class)))
        .thenReturn(List.of(definition));
    when(assignmentDao.findByExperimentDefinitionIdAndAssignmentUnitId(any(), any()))
        .thenReturn(Optional.empty());
    when(assignmentDao.findByExclusionGroupAndExclusionWindowKeyAndAssignmentUnitId(
            "ranking", "ranking-summer-2026", unitId))
        .thenReturn(
            Optional.of(
                assignment(definition("other", "ranking-summer-2026"), "control", "rules.v1")));

    assertThatThrownBy(
            () ->
                service.assign(
                    new ExperimentAssignmentCommand("rankingPilotB", unitId, NOW.minusSeconds(1))))
        .isInstanceOf(ExperimentAssignmentException.class)
        .hasMessage("EXPERIMENT_MUTUAL_EXCLUSION_CONFLICT");
    verify(assignmentDao, never()).saveAndFlush(any());
  }

  @Test
  void bindsExposureIdempotentlyOnlyWhenDecisionMatchesAssignment() {
    ExperimentDefinitionEntity definition = definition("rankingPilot", "ranking-summer-2026");
    ExperimentAssignmentEntity assignment = assignment(definition, "treatment", "hybrid.v1");
    RecommendationRequestEntity request = new RecommendationRequestEntity();
    request.setId(UUID.randomUUID());
    request.setRequestId(UUID.randomUUID());
    request.setExperimentKey("rankingPilot");
    request.setVariantKey("treatment");
    request.setPolicyVersion("hybrid.v1");
    when(assignmentDao.findById(assignment.getId())).thenReturn(Optional.of(assignment));
    when(requestDao.findByRequestId(request.getRequestId())).thenReturn(Optional.of(request));
    Instant exposureAt = NOW.minusSeconds(1);

    ExperimentAssignmentResult result =
        service.registerExposure(
            new ExperimentExposureCommand(assignment.getId(), request.getRequestId(), exposureAt));
    ExperimentAssignmentResult retry =
        service.registerExposure(
            new ExperimentExposureCommand(assignment.getId(), request.getRequestId(), exposureAt));

    assertThat(result.exposureRecordedAt()).isEqualTo(exposureAt);
    assertThat(retry).isEqualTo(result);
    assertThat(assignment.getRecommendationRequest()).isSameAs(request);
    verify(assignmentDao).saveAndFlush(assignment);
  }

  @Test
  void rejectsExposureBeforeAssignmentOrWithAChangedPolicy() {
    ExperimentDefinitionEntity definition = definition("rankingPilot", "ranking-summer-2026");
    ExperimentAssignmentEntity assignment = assignment(definition, "treatment", "hybrid.v1");
    RecommendationRequestEntity request = new RecommendationRequestEntity();
    request.setId(UUID.randomUUID());
    request.setRequestId(UUID.randomUUID());
    request.setExperimentKey("rankingPilot");
    request.setVariantKey("treatment");
    request.setPolicyVersion("hybrid.v2");
    when(assignmentDao.findById(assignment.getId())).thenReturn(Optional.of(assignment));
    when(requestDao.findByRequestId(request.getRequestId())).thenReturn(Optional.of(request));

    assertThatThrownBy(
            () ->
                service.registerExposure(
                    new ExperimentExposureCommand(
                        assignment.getId(), request.getRequestId(), NOW.minusSeconds(20))))
        .isInstanceOf(ExperimentAssignmentException.class)
        .hasMessage("EXPERIMENT_EXPOSURE_MISMATCH");
    verify(assignmentDao, never()).saveAndFlush(any());
  }

  private ExperimentDefinitionEntity definition(String key, String window) {
    ExperimentDefinitionEntity definition = new ExperimentDefinitionEntity();
    definition.setId(UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    definition.setExperimentKey(key);
    definition.setVersion(1);
    definition.setExclusionGroup("ranking");
    definition.setExclusionWindowKey(window);
    definition.setControlVariantKey("control");
    definition.setTreatmentVariantKey("treatment");
    definition.setControlPolicyVersion("rules.v1");
    definition.setTreatmentPolicyVersion("hybrid.v1");
    definition.setTreatmentAllocationBps(5_000);
    definition.setAssignmentSaltVersion("salt.v1");
    return definition;
  }

  private ExperimentAssignmentEntity assignment(
      ExperimentDefinitionEntity definition, String variant, String policy) {
    ExperimentAssignmentEntity assignment = new ExperimentAssignmentEntity();
    assignment.setId(UUID.randomUUID());
    assignment.setExperimentDefinition(definition);
    assignment.setAssignmentUnitId(UUID.randomUUID());
    assignment.setExclusionGroup("ranking");
    assignment.setExclusionWindowKey(definition.getExclusionWindowKey());
    assignment.setVariantKey(variant);
    assignment.setPolicyVersion(policy);
    assignment.setBucket(8_000);
    assignment.setAssignedAt(NOW.minusSeconds(10));
    assignment.setCreatedAt(NOW);
    return assignment;
  }
}
