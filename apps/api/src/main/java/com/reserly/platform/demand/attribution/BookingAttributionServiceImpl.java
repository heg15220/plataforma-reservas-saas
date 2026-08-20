package com.reserly.platform.demand.attribution;

import com.reserly.platform.demand.attribution.persistence.BookingAttributionDao;
import com.reserly.platform.demand.attribution.persistence.BookingAttributionEntity;
import com.reserly.platform.demand.event.persistence.BehaviorEventDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construye la proyección de atribución desde eventos correlacionados y snapshots V47.
 *
 * <p>La reserva y su email permanecen en el dominio operativo. Solo se persiste si el cliente ya
 * existía, nunca su identidad. Un replay devuelve la fila existente sin cambiar evidencia o
 * versión.
 */
@Service
public class BookingAttributionServiceImpl implements BookingAttributionService {

  private final BookingAttributionDao attributionDao;
  private final ReservationDao reservationDao;
  private final BehaviorEventDao eventDao;
  private final RecommendationRequestDao recommendationRequestDao;
  private final RecommendationCandidateDao candidateDao;
  private final BookingAttributionClassifier classifier;
  private final Clock clock;

  public BookingAttributionServiceImpl(
      BookingAttributionDao attributionDao,
      ReservationDao reservationDao,
      BehaviorEventDao eventDao,
      RecommendationRequestDao recommendationRequestDao,
      RecommendationCandidateDao candidateDao,
      BookingAttributionClassifier classifier,
      Clock clock) {
    this.attributionDao = attributionDao;
    this.reservationDao = reservationDao;
    this.eventDao = eventDao;
    this.recommendationRequestDao = recommendationRequestDao;
    this.candidateDao = candidateDao;
    this.classifier = classifier;
    this.clock = clock;
  }

  /** Ejecuta una transición idempotente; la constraint única protege carreras entre listeners. */
  @Override
  @Transactional
  public BookingAttributionEntity attribute(
      UUID reservationId, UUID requestId, Instant confirmedAt) {
    if (reservationId == null || requestId == null || confirmedAt == null) {
      throw new IllegalArgumentException("BOOKING_ATTRIBUTION_REQUEST_INVALID");
    }
    Optional<BookingAttributionEntity> existing = attributionDao.findByReservationId(reservationId);
    if (existing.isPresent()) {
      return existing.get();
    }
    ReservationEntity reservation =
        reservationDao
            .findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("BOOKING_ATTRIBUTION_NOT_FOUND"));
    if (!isAttributable(reservation.getStatus())) {
      throw new IllegalArgumentException("BOOKING_ATTRIBUTION_STATUS_INVALID");
    }

    BookingAttributionDecision decision =
        classifier.classify(
            reservation.getVenue().getId(),
            confirmedAt,
            eventDao.findByRequestIdOrdered(requestId));
    Optional<RecommendationRequestEntity> recommendation =
        recommendationRequestDao.findByRequestId(requestId);
    Optional<RecommendationCandidateEntity> visibleCandidate =
        recommendation.flatMap(
            request ->
                candidateDao.findVisibleByRequestIdAndVenue(
                    request.getId(), reservation.getVenue().getId()));

    BookingAttributionEntity attribution = new BookingAttributionEntity();
    attribution.setReservation(reservation);
    attribution.setVenueId(reservation.getVenue().getId());
    attribution.setRecommendationRequest(recommendation.orElse(null));
    attribution.setRequestId(requestId);
    attribution.setAttributionClass(decision.attributionClass());
    attribution.setReasonCode(decision.reasonCode());
    attribution.setPolicyVersion(BookingAttributionPolicy.VERSION);
    attribution.setWindowStartedAt(confirmedAt.minus(BookingAttributionPolicy.WINDOW));
    attribution.setWindowEndedAt(confirmedAt);
    attribution.setConfidence(BigDecimal.valueOf(decision.confidence()));
    attribution.setNewCustomer(
        !reservationDao.existsPriorConfirmedCustomer(
            reservation.getVenue().getId(),
            reservation.getCustomerEmailNormalized(),
            reservation.getId(),
            confirmedAt));
    applyVisibleMoney(attribution, decision.attributionClass(), visibleCandidate.orElse(null));
    attribution.setEvidenceJson(
        Map.of(
            "eventIds",
            decision.evidence().stream().map(event -> event.getEventId().toString()).toList(),
            "eventTypes",
            decision.evidence().stream().map(event -> event.getEventType()).toList()));
    Instant classifiedAt = clock.instant();
    if (classifiedAt.isBefore(confirmedAt)) {
      classifiedAt = confirmedAt;
    }
    attribution.setClassifiedAt(classifiedAt);
    attribution.setCreatedAt(classifiedAt);
    return attributionDao.save(attribution);
  }

  private static boolean isAttributable(String status) {
    return List.of("confirmed", "attended", "no_show", "reported").contains(status);
  }

  private static void applyVisibleMoney(
      BookingAttributionEntity attribution,
      String attributionClass,
      RecommendationCandidateEntity candidate) {
    if ("direct".equals(attributionClass)
        || candidate == null
        || candidate.getObservedPrice() == null) {
      return;
    }
    attribution.setAttributedAmount(candidate.getObservedPrice());
    attribution.setAttributedCurrency(candidate.getObservedCurrency());
  }
}
