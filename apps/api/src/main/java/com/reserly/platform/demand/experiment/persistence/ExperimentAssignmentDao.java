package com.reserly.platform.demand.experiment.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso idempotente a asignaciones y a sus registros previos de exposición. */
public interface ExperimentAssignmentDao extends JpaRepository<ExperimentAssignmentEntity, UUID> {
  Optional<ExperimentAssignmentEntity> findByExperimentDefinitionIdAndAssignmentUnitId(
      UUID experimentDefinitionId, UUID assignmentUnitId);

  Optional<ExperimentAssignmentEntity> findByExclusionGroupAndExclusionWindowKeyAndAssignmentUnitId(
      String exclusionGroup, String exclusionWindowKey, UUID assignmentUnitId);

  Optional<ExperimentAssignmentEntity> findByRecommendationRequestId(UUID recommendationRequestId);
}
