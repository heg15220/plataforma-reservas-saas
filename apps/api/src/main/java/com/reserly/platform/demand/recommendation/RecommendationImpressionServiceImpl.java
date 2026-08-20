package com.reserly.platform.demand.recommendation;

import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentDao;
import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentEntity;
import com.reserly.platform.demand.ingestion.DemandEventIngestionService;
import com.reserly.platform.demand.ingestion.EventIngestionRequest;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRankingDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRankingEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida una impresión contra la fotografía transaccional del conjunto y su ranking.
 *
 * <p>El consumidor solo aporta IDs realmente renderizados. Las posiciones, explicaciones, versiones
 * y señales observables proceden del agregado persistido; nunca acepta scores o atributos libres
 * del cliente. La validación completa precede a cualquier escritura para evitar impresiones
 * parciales.
 */
@Service
public class RecommendationImpressionServiceImpl implements RecommendationImpressionService {

  private static final int MAX_VISIBLE_CANDIDATES = 100;

  private final RecommendationRequestDao requestDao;
  private final RecommendationCandidateDao candidateDao;
  private final RecommendationRankingDao rankingDao;
  private final ExperimentAssignmentDao experimentAssignmentDao;
  private final DemandEventIngestionService ingestionService;
  private final Clock clock;

  public RecommendationImpressionServiceImpl(
      RecommendationRequestDao requestDao,
      RecommendationCandidateDao candidateDao,
      RecommendationRankingDao rankingDao,
      ExperimentAssignmentDao experimentAssignmentDao,
      DemandEventIngestionService ingestionService,
      Clock clock) {
    this.requestDao = requestDao;
    this.candidateDao = candidateDao;
    this.rankingDao = rankingDao;
    this.experimentAssignmentDao = experimentAssignmentDao;
    this.ingestionService = ingestionService;
    this.clock = clock;
  }

  @Override
  @Transactional
  public RecommendationImpressionResult record(RecommendationImpressionCommand command) {
    validateEnvelope(command);
    RecommendationRequestEntity request =
        requestDao
            .findByRequestId(command.recommendationRequestId())
            .orElseThrow(RecommendationImpressionException::new);
    requireRegisteredExperimentExposure(request, command.occurredAt());
    Map<UUID, RecommendationCandidateEntity> candidates = candidatesById(request.getId());
    Map<UUID, RecommendationRankingEntity> rankings = rankingsByCandidateId(request.getId());

    List<RecommendationCandidateEntity> visible =
        command.candidateIds().stream()
            .map(candidateId -> requireVisibleCandidate(candidateId, candidates, rankings))
            .toList();

    for (RecommendationCandidateEntity candidate : visible) {
      RecommendationRankingEntity ranking = rankings.get(candidate.getId());
      candidate.setWasVisible(true);
      ingestionService.ingestTrusted(toEvent(command, request, candidate, ranking));
    }
    candidateDao.saveAll(visible);
    return new RecommendationImpressionResult(
        command.impressionId(),
        command.recommendationRequestId(),
        List.copyOf(command.candidateIds()));
  }

  /**
   * Impide que una superficie experimental sea observable sin asignación y exposición durables. Las
   * decisiones fuera de experimento conservan el flujo normal.
   */
  private void requireRegisteredExperimentExposure(
      RecommendationRequestEntity request, java.time.Instant impressionAt) {
    if (request.getExperimentKey() == null) {
      return;
    }
    ExperimentAssignmentEntity assignment =
        experimentAssignmentDao
            .findByRecommendationRequestId(request.getId())
            .orElseThrow(RecommendationImpressionException::new);
    if (assignment.getExposureRecordedAt() == null
        || assignment.getExposureRecordedAt().isAfter(impressionAt)
        || !assignment
            .getExperimentDefinition()
            .getExperimentKey()
            .equals(request.getExperimentKey())
        || !assignment.getVariantKey().equals(request.getVariantKey())
        || !assignment.getPolicyVersion().equals(request.getPolicyVersion())) {
      throw new RecommendationImpressionException();
    }
  }

  private void validateEnvelope(RecommendationImpressionCommand command) {
    if (command == null
        || command.impressionId() == null
        || command.recommendationRequestId() == null
        || command.occurredAt() == null
        || command.occurredAt().isAfter(clock.instant())
        || command.candidateIds() == null
        || command.candidateIds().isEmpty()
        || command.candidateIds().size() > MAX_VISIBLE_CANDIDATES
        || command.candidateIds().stream().anyMatch(java.util.Objects::isNull)
        || new HashSet<>(command.candidateIds()).size() != command.candidateIds().size()) {
      throw new RecommendationImpressionException();
    }
  }

  private Map<UUID, RecommendationCandidateEntity> candidatesById(UUID requestId) {
    Map<UUID, RecommendationCandidateEntity> result = new HashMap<>();
    candidateDao
        .findAllByRequestIdOrdered(requestId)
        .forEach(candidate -> result.put(candidate.getId(), candidate));
    return result;
  }

  private Map<UUID, RecommendationRankingEntity> rankingsByCandidateId(UUID requestId) {
    Map<UUID, RecommendationRankingEntity> result = new HashMap<>();
    rankingDao
        .findByRequestIdOrdered(requestId)
        .forEach(ranking -> result.put(ranking.getRecommendationCandidate().getId(), ranking));
    return result;
  }

  private RecommendationCandidateEntity requireVisibleCandidate(
      UUID candidateId,
      Map<UUID, RecommendationCandidateEntity> candidates,
      Map<UUID, RecommendationRankingEntity> rankings) {
    RecommendationCandidateEntity candidate = candidates.get(candidateId);
    if (candidate == null
        || !"eligible".equals(candidate.getEligibilityStatus())
        || !candidate.isObservedAvailability()
        || !rankings.containsKey(candidateId)) {
      throw new RecommendationImpressionException();
    }
    return candidate;
  }

  private EventIngestionRequest toEvent(
      RecommendationImpressionCommand command,
      RecommendationRequestEntity request,
      RecommendationCandidateEntity candidate,
      RecommendationRankingEntity ranking) {
    return new EventIngestionRequest(
        deterministicEventId(command.impressionId(), candidate.getId()),
        (short) 1,
        "recommendationShown",
        command.occurredAt(),
        command.recommendationRequestId(),
        request.getPurpose(),
        request.getConsentVersion(),
        request.getSessionId(),
        request.getAnonymousIdentity() == null ? null : request.getAnonymousIdentity().getId(),
        request.getCustomerIdentity() == null ? null : request.getCustomerIdentity().getId(),
        candidate.getVenueId(),
        null,
        null,
        null,
        null,
        Map.of(
            "activationId", command.impressionId().toString(),
            "position", ranking.getFinalPosition(),
            "policyVersion", toEventCode(ranking.getPolicyVersion()),
            "explanationCode", toEventCode(ranking.getExplanationCode())));
  }

  private UUID deterministicEventId(UUID impressionId, UUID candidateId) {
    return UUID.nameUUIDFromBytes(
        ("recommendation-impression:" + impressionId + ':' + candidateId)
            .getBytes(StandardCharsets.UTF_8));
  }

  private String toEventCode(String value) {
    StringBuilder result = new StringBuilder();
    boolean uppercaseNext = false;
    for (char character : value.toCharArray()) {
      if (!Character.isLetterOrDigit(character)) {
        uppercaseNext = result.length() > 0;
      } else if (result.length() < 64) {
        char normalized = Character.toLowerCase(character);
        result.append(uppercaseNext ? Character.toUpperCase(normalized) : normalized);
        uppercaseNext = false;
      }
    }
    if (result.isEmpty() || !Character.isLetter(result.charAt(0))) {
      throw new RecommendationImpressionException();
    }
    return result.toString();
  }
}
