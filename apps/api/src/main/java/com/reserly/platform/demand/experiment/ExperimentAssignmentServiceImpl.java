package com.reserly.platform.demand.experiment;

import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentDao;
import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentEntity;
import com.reserly.platform.demand.experiment.persistence.ExperimentDefinitionDao;
import com.reserly.platform.demand.experiment.persistence.ExperimentDefinitionEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementa bucketing SHA-256 estable, exclusión mutua durable y exposición previa al render.
 *
 * <p>El hash usa exclusivamente códigos versionados y un UUID seudónimo. Los conflictos de unicidad
 * se cierran de forma segura: nunca se reasigna silenciosamente a otra variante.
 */
@Service
public class ExperimentAssignmentServiceImpl implements ExperimentAssignmentService {

  private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");
  private static final int BUCKET_COUNT = 10_000;

  private final ExperimentDefinitionDao definitionDao;
  private final ExperimentAssignmentDao assignmentDao;
  private final RecommendationRequestDao requestDao;
  private final Clock clock;

  public ExperimentAssignmentServiceImpl(
      ExperimentDefinitionDao definitionDao,
      ExperimentAssignmentDao assignmentDao,
      RecommendationRequestDao requestDao,
      Clock clock) {
    this.definitionDao = definitionDao;
    this.assignmentDao = assignmentDao;
    this.requestDao = requestDao;
    this.clock = clock;
  }

  @Override
  @Transactional
  public ExperimentAssignmentResult assign(ExperimentAssignmentCommand command) {
    validateAssignmentCommand(command);
    ExperimentDefinitionEntity definition = activeDefinition(command);
    var existing =
        assignmentDao.findByExperimentDefinitionIdAndAssignmentUnitId(
            definition.getId(), command.assignmentUnitId());
    if (existing.isPresent()) {
      return toResult(existing.orElseThrow());
    }
    assignmentDao
        .findByExclusionGroupAndExclusionWindowKeyAndAssignmentUnitId(
            definition.getExclusionGroup(),
            definition.getExclusionWindowKey(),
            command.assignmentUnitId())
        .ifPresent(
            conflicting -> {
              throw new ExperimentAssignmentException("EXPERIMENT_MUTUAL_EXCLUSION_CONFLICT");
            });

    int bucket = stableBucket(definition, command.assignmentUnitId());
    boolean treatment = bucket < definition.getTreatmentAllocationBps();
    ExperimentAssignmentEntity assignment = new ExperimentAssignmentEntity();
    assignment.setExperimentDefinition(definition);
    assignment.setAssignmentUnitId(command.assignmentUnitId());
    assignment.setExclusionGroup(definition.getExclusionGroup());
    assignment.setExclusionWindowKey(definition.getExclusionWindowKey());
    assignment.setVariantKey(
        treatment ? definition.getTreatmentVariantKey() : definition.getControlVariantKey());
    assignment.setPolicyVersion(
        treatment ? definition.getTreatmentPolicyVersion() : definition.getControlPolicyVersion());
    assignment.setBucket(bucket);
    assignment.setAssignedAt(command.assignedAt());
    assignment.setCreatedAt(clock.instant());
    try {
      return toResult(assignmentDao.saveAndFlush(assignment));
    } catch (DataIntegrityViolationException conflict) {
      // La constraint decide la carrera; un reintento posterior recuperará la fila ganadora.
      throw new ExperimentAssignmentException("EXPERIMENT_ASSIGNMENT_CONFLICT");
    }
  }

  @Override
  @Transactional
  public ExperimentAssignmentResult registerExposure(ExperimentExposureCommand command) {
    if (command == null
        || command.assignmentId() == null
        || command.recommendationRequestId() == null
        || command.exposedAt() == null
        || command.exposedAt().isAfter(clock.instant())) {
      throw new ExperimentAssignmentException("EXPERIMENT_EXPOSURE_INVALID");
    }
    ExperimentAssignmentEntity assignment =
        assignmentDao
            .findById(command.assignmentId())
            .orElseThrow(
                () -> new ExperimentAssignmentException("EXPERIMENT_ASSIGNMENT_NOT_FOUND"));
    RecommendationRequestEntity request =
        requestDao
            .findByRequestId(command.recommendationRequestId())
            .orElseThrow(() -> new ExperimentAssignmentException("EXPERIMENT_REQUEST_NOT_FOUND"));
    validateExposure(assignment, request, command.exposedAt());
    if (assignment.getRecommendationRequest() != null) {
      if (!assignment.getRecommendationRequest().getId().equals(request.getId())
          || !assignment.getExposureRecordedAt().equals(command.exposedAt())) {
        throw new ExperimentAssignmentException("EXPERIMENT_EXPOSURE_ALREADY_BOUND");
      }
      return toResult(assignment);
    }
    assignment.setRecommendationRequest(request);
    assignment.setExposureRecordedAt(command.exposedAt());
    return toResult(assignmentDao.saveAndFlush(assignment));
  }

  private ExperimentDefinitionEntity activeDefinition(ExperimentAssignmentCommand command) {
    List<ExperimentDefinitionEntity> active =
        definitionDao.findActive(
            command.experimentKey(), command.assignedAt(), PageRequest.of(0, 1));
    if (active.isEmpty()) {
      throw new ExperimentAssignmentException("EXPERIMENT_NOT_ACTIVE");
    }
    return active.get(0);
  }

  private void validateAssignmentCommand(ExperimentAssignmentCommand command) {
    if (command == null
        || command.experimentKey() == null
        || !CODE.matcher(command.experimentKey()).matches()
        || command.assignmentUnitId() == null
        || command.assignedAt() == null
        || command.assignedAt().isAfter(clock.instant())) {
      throw new ExperimentAssignmentException("EXPERIMENT_ASSIGNMENT_INVALID");
    }
  }

  private void validateExposure(
      ExperimentAssignmentEntity assignment,
      RecommendationRequestEntity request,
      Instant exposedAt) {
    ExperimentDefinitionEntity definition = assignment.getExperimentDefinition();
    if (exposedAt.isBefore(assignment.getAssignedAt())
        || !definition.getExperimentKey().equals(request.getExperimentKey())
        || !assignment.getVariantKey().equals(request.getVariantKey())
        || !assignment.getPolicyVersion().equals(request.getPolicyVersion())) {
      throw new ExperimentAssignmentException("EXPERIMENT_EXPOSURE_MISMATCH");
    }
  }

  /** Deriva 10 000 buckets con bytes sin signo; no depende de hashCode ni del proceso. */
  private int stableBucket(ExperimentDefinitionEntity definition, UUID assignmentUnitId) {
    String material =
        definition.getExperimentKey()
            + ':'
            + definition.getVersion()
            + ':'
            + definition.getAssignmentSaltVersion()
            + ':'
            + assignmentUnitId;
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
      long unsigned = Integer.toUnsignedLong(ByteBuffer.wrap(digest).getInt());
      return (int) (unsigned % BUCKET_COUNT);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 no disponible", impossible);
    }
  }

  private ExperimentAssignmentResult toResult(ExperimentAssignmentEntity assignment) {
    ExperimentDefinitionEntity definition = assignment.getExperimentDefinition();
    return new ExperimentAssignmentResult(
        assignment.getId(),
        definition.getExperimentKey(),
        definition.getVersion(),
        assignment.getVariantKey(),
        assignment.getPolicyVersion(),
        assignment.getBucket(),
        assignment.getAssignedAt(),
        assignment.getExposureRecordedAt());
  }
}
