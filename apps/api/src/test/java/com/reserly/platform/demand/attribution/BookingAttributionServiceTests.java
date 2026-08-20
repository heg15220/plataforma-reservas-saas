package com.reserly.platform.demand.attribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.attribution.persistence.BookingAttributionDao;
import com.reserly.platform.demand.attribution.persistence.BookingAttributionEntity;
import com.reserly.platform.demand.event.persistence.BehaviorEventDao;
import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationCandidateEntity;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestDao;
import com.reserly.platform.demand.recommendation.persistence.RecommendationRequestEntity;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica idempotencia, minimización, nuevo cliente y precio visible atribuido. */
@ExtendWith(MockitoExtension.class)
class BookingAttributionServiceTests {

  private static final Instant NOW = Instant.parse("2026-08-20T09:00:00Z");
  @Mock private BookingAttributionDao attributionDao;
  @Mock private ReservationDao reservationDao;
  @Mock private BehaviorEventDao eventDao;
  @Mock private RecommendationRequestDao recommendationRequestDao;
  @Mock private RecommendationCandidateDao candidateDao;
  private BookingAttributionServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new BookingAttributionServiceImpl(
            attributionDao,
            reservationDao,
            eventDao,
            recommendationRequestDao,
            candidateDao,
            new BookingAttributionClassifier(),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void persistsGeneratedAttributionWithVisibleMoneyAndNoIdentity() {
    UUID reservationId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId, venueId);
    BehaviorEventEntity shown = event("recommendationShown", venueId);
    RecommendationRequestEntity recommendation = new RecommendationRequestEntity();
    recommendation.setId(UUID.randomUUID());
    RecommendationCandidateEntity candidate = new RecommendationCandidateEntity();
    candidate.setObservedPrice(new BigDecimal("35.00"));
    candidate.setObservedCurrency("EUR");
    when(reservationDao.findById(reservationId)).thenReturn(Optional.of(reservation));
    when(eventDao.findByRequestIdOrdered(requestId)).thenReturn(List.of(shown));
    when(recommendationRequestDao.findByRequestId(requestId))
        .thenReturn(Optional.of(recommendation));
    when(candidateDao.findVisibleByRequestIdAndVenue(recommendation.getId(), venueId))
        .thenReturn(Optional.of(candidate));
    when(reservationDao.existsPriorConfirmedCustomer(
            venueId, "client@example.com", reservationId, NOW))
        .thenReturn(false);
    when(attributionDao.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    BookingAttributionEntity result = service.attribute(reservationId, requestId, NOW);

    assertThat(result.getAttributionClass()).isEqualTo("generated");
    assertThat(result.isNewCustomer()).isTrue();
    assertThat(result.getAttributedAmount()).isEqualByComparingTo("35.00");
    assertThat(result.getAttributedCurrency()).isEqualTo("EUR");
    assertThat(result.getEvidenceJson()).containsOnlyKeys("eventIds", "eventTypes");
  }

  @Test
  void replayReturnsExistingWithoutReadingOperationalData() {
    UUID reservationId = UUID.randomUUID();
    BookingAttributionEntity existing = new BookingAttributionEntity();
    when(attributionDao.findByReservationId(reservationId)).thenReturn(Optional.of(existing));

    assertThat(service.attribute(reservationId, UUID.randomUUID(), NOW)).isSameAs(existing);
    verify(reservationDao, never()).findById(reservationId);
    verify(attributionDao, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private static ReservationEntity reservation(UUID id, UUID venueId) {
    VenueEntity venue = new VenueEntity();
    venue.setId(venueId);
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(id);
    reservation.setVenue(venue);
    reservation.setStatus("confirmed");
    reservation.setCustomerEmailNormalized("client@example.com");
    return reservation;
  }

  private static BehaviorEventEntity event(String type, UUID venueId) {
    BehaviorEventEntity event = new BehaviorEventEntity();
    event.setEventId(UUID.randomUUID());
    event.setEventType(type);
    event.setOccurredAt(NOW.minusSeconds(60));
    event.setVenueId(venueId);
    return event;
  }
}
