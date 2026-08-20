package com.reserly.platform.demand.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentDao;
import com.reserly.platform.demand.experiment.persistence.ExperimentAssignmentEntity;
import com.reserly.platform.demand.experiment.persistence.ExperimentDefinitionEntity;
import com.reserly.platform.demand.ingestion.DemandEventIngestionService;
import com.reserly.platform.demand.ingestion.EventIngestionRequest;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRankingDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRankingEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Verifica que una impresión no pueda reintroducir alternativas ni atributos ocultos. */
class RecommendationImpressionServiceTests {

  private static final Instant NOW = Instant.parse("2026-08-13T14:00:00Z");

  private RecommendationRequestDao requestDao;
  private RecommendationCandidateDao candidateDao;
  private RecommendationRankingDao rankingDao;
  private DemandEventIngestionService ingestionService;
  private ExperimentAssignmentDao experimentAssignmentDao;
  private RecommendationImpressionService service;
  private UUID publicRequestId;
  private UUID internalRequestId;

  @BeforeEach
  void setUp() {
    requestDao = mock(RecommendationRequestDao.class);
    candidateDao = mock(RecommendationCandidateDao.class);
    rankingDao = mock(RecommendationRankingDao.class);
    ingestionService = mock(DemandEventIngestionService.class);
    experimentAssignmentDao = mock(ExperimentAssignmentDao.class);
    service =
        new RecommendationImpressionServiceImpl(
            requestDao,
            candidateDao,
            rankingDao,
            experimentAssignmentDao,
            ingestionService,
            Clock.fixed(NOW, ZoneOffset.UTC));
    publicRequestId = UUID.randomUUID();
    internalRequestId = UUID.randomUUID();
  }

  @Test
  void requiresARegisteredAssignmentBeforeAnExperimentalImpression() {
    RecommendationRequestEntity request = request();
    request.setExperimentKey("rankingPilot");
    request.setVariantKey("treatment");
    request.setPolicyVersion("hybrid.v1");
    RecommendationCandidateEntity candidate = candidate("eligible", true);
    stubAggregate(request, List.of(candidate), List.of(ranking(candidate, 1)));

    RecommendationImpressionCommand command =
        new RecommendationImpressionCommand(
            UUID.randomUUID(), publicRequestId, List.of(candidate.getId()), NOW.minusSeconds(1));
    assertThatThrownBy(() -> service.record(command))
        .isInstanceOf(RecommendationImpressionException.class);

    ExperimentDefinitionEntity definition = new ExperimentDefinitionEntity();
    definition.setExperimentKey("rankingPilot");
    ExperimentAssignmentEntity assignment = new ExperimentAssignmentEntity();
    assignment.setExperimentDefinition(definition);
    assignment.setVariantKey("treatment");
    assignment.setPolicyVersion("hybrid.v1");
    assignment.setExposureRecordedAt(NOW.minusSeconds(2));
    when(experimentAssignmentDao.findByRecommendationRequestId(internalRequestId))
        .thenReturn(Optional.of(assignment));

    service.record(command);

    assertThat(candidate.isWasVisible()).isTrue();
    verify(ingestionService).ingestTrusted(any());
  }

  @Test
  void recordsOnlyPersistedEligibleRankedCandidatesWithObservableContext() {
    RecommendationRequestEntity request = request();
    RecommendationCandidateEntity first = candidate("eligible", true);
    RecommendationCandidateEntity second = candidate("eligible", true);
    stubAggregate(request, List.of(first, second), List.of(ranking(first, 1), ranking(second, 2)));
    UUID impressionId = UUID.randomUUID();

    RecommendationImpressionResult result =
        service.record(
            new RecommendationImpressionCommand(
                impressionId,
                publicRequestId,
                List.of(first.getId(), second.getId()),
                NOW.minusSeconds(1)));

    assertThat(result.visibleCandidateIds()).containsExactly(first.getId(), second.getId());
    assertThat(first.isWasVisible()).isTrue();
    assertThat(second.isWasVisible()).isTrue();
    ArgumentCaptor<EventIngestionRequest> events =
        ArgumentCaptor.forClass(EventIngestionRequest.class);
    verify(ingestionService, org.mockito.Mockito.times(2)).ingestTrusted(events.capture());
    assertThat(events.getAllValues())
        .allSatisfy(
            event -> {
              assertThat(event.eventType()).isEqualTo("recommendationShown");
              assertThat(event.requestId()).isEqualTo(publicRequestId);
              assertThat(event.context().keySet())
                  .containsExactlyInAnyOrder(
                      "activationId", "position", "policyVersion", "explanationCode");
              assertThat(event.context()).doesNotContainKeys("score", "scoreComponents", "email");
            });
    verify(candidateDao).saveAll(List.of(first, second));
  }

  @Test
  void rejectsTheWholeImpressionWhenOneCandidateIsIneligibleOrUnavailable() {
    RecommendationRequestEntity request = request();
    RecommendationCandidateEntity eligible = candidate("eligible", true);
    RecommendationCandidateEntity forbidden = candidate("ineligible", false);
    stubAggregate(
        request,
        List.of(eligible, forbidden),
        List.of(ranking(eligible, 1), ranking(forbidden, 2)));

    RecommendationImpressionCommand command =
        new RecommendationImpressionCommand(
            UUID.randomUUID(),
            publicRequestId,
            List.of(eligible.getId(), forbidden.getId()),
            NOW.minusSeconds(1));

    assertThatThrownBy(() -> service.record(command))
        .isInstanceOf(RecommendationImpressionException.class)
        .hasMessage("RECOMMENDATION_IMPRESSION_INVALID");
    assertThat(eligible.isWasVisible()).isFalse();
    verify(ingestionService, never()).ingestTrusted(any());
    verify(candidateDao, never()).saveAll(any());
  }

  @Test
  void rejectsUnknownUnrankedDuplicatedAndFutureCandidatesBeforeWriting() {
    RecommendationRequestEntity request = request();
    RecommendationCandidateEntity unranked = candidate("eligible", true);
    stubAggregate(request, List.of(unranked), List.of());

    List<RecommendationImpressionCommand> invalid =
        List.of(
            new RecommendationImpressionCommand(
                UUID.randomUUID(), publicRequestId, List.of(UUID.randomUUID()), NOW),
            new RecommendationImpressionCommand(
                UUID.randomUUID(), publicRequestId, List.of(unranked.getId()), NOW),
            new RecommendationImpressionCommand(
                UUID.randomUUID(),
                publicRequestId,
                List.of(unranked.getId(), unranked.getId()),
                NOW),
            new RecommendationImpressionCommand(
                UUID.randomUUID(), publicRequestId, List.of(unranked.getId()), NOW.plusSeconds(1)));

    invalid.forEach(
        command ->
            assertThatThrownBy(() -> service.record(command))
                .isInstanceOf(RecommendationImpressionException.class));
    verify(ingestionService, never()).ingestTrusted(any());
    verify(candidateDao, never()).saveAll(any());
  }

  @Test
  void derivesStableEventIdsForSafeImpressionRetries() {
    RecommendationRequestEntity request = request();
    RecommendationCandidateEntity candidate = candidate("eligible", true);
    stubAggregate(request, List.of(candidate), List.of(ranking(candidate, 1)));
    RecommendationImpressionCommand command =
        new RecommendationImpressionCommand(
            UUID.randomUUID(), publicRequestId, List.of(candidate.getId()), NOW.minusSeconds(1));

    service.record(command);
    service.record(command);

    ArgumentCaptor<EventIngestionRequest> events =
        ArgumentCaptor.forClass(EventIngestionRequest.class);
    verify(ingestionService, org.mockito.Mockito.times(2)).ingestTrusted(events.capture());
    assertThat(events.getAllValues().get(0).eventId())
        .isEqualTo(events.getAllValues().get(1).eventId());
  }

  private RecommendationRequestEntity request() {
    RecommendationRequestEntity request = new RecommendationRequestEntity();
    request.setId(internalRequestId);
    request.setRequestId(publicRequestId);
    request.setPurpose("analytics");
    request.setPolicyVersion("rules.v1");
    return request;
  }

  private RecommendationCandidateEntity candidate(String eligibility, boolean availability) {
    RecommendationCandidateEntity candidate = new RecommendationCandidateEntity();
    candidate.setId(UUID.randomUUID());
    candidate.setVenueId(UUID.randomUUID());
    candidate.setEligibilityStatus(eligibility);
    candidate.setObservedAvailability(availability);
    candidate.setVisibleSignalsJson(Map.of("categoryCode", "peluqueria", "rating", 4.8));
    return candidate;
  }

  private RecommendationRankingEntity ranking(
      RecommendationCandidateEntity candidate, int position) {
    RecommendationRankingEntity ranking = new RecommendationRankingEntity();
    ranking.setRecommendationCandidate(candidate);
    ranking.setFinalPosition(position);
    ranking.setPolicyVersion("rules.v1");
    ranking.setExplanationCode("AVAILABLE_NEARBY");
    return ranking;
  }

  private void stubAggregate(
      RecommendationRequestEntity request,
      List<RecommendationCandidateEntity> candidates,
      List<RecommendationRankingEntity> rankings) {
    when(requestDao.findByRequestId(publicRequestId)).thenReturn(Optional.of(request));
    when(candidateDao.findAllByRequestIdOrdered(internalRequestId)).thenReturn(candidates);
    when(rankingDao.findByRequestIdOrdered(internalRequestId)).thenReturn(rankings);
  }
}
