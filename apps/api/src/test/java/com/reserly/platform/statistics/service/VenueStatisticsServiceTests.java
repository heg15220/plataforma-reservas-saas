package com.reserly.platform.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.statistics.persistence.StatsDailyVenueDao;
import com.reserly.platform.statistics.persistence.StatsDailyVenueEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica filtros, aislamiento por propietario y cálculos derivados del periodo. */
class VenueStatisticsServiceTests {

  private static final UUID OWNER_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final UUID VENUE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
  private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Europe/Madrid"));

  @Test
  void recalculatesCurrentMonthAndBuildsWeightedMetrics() {
    VenueDao venueDao = mock(VenueDao.class);
    StatsDailyVenueDao statsDao = mock(StatsDailyVenueDao.class);
    VenueEntity venue = new VenueEntity();
    venue.setId(VENUE_ID);
    when(venueDao.findCurrentByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(venue));
    List<StatsDailyVenueEntity> days =
        List.of(
            stats(LocalDate.of(2026, 7, 1), 4, 3, 1, 1, 2, 6, 10, 1, 2, "5.00"),
            stats(LocalDate.of(2026, 7, 2), 6, 5, 1, 2, 3, 9, 20, 3, 1, "3.00"));
    when(statsDao.findRange(VENUE_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 29)))
        .thenReturn(days);
    DemandCommercialMetricsAssembler demandMetrics = mock(DemandCommercialMetricsAssembler.class);
    var service = new VenueStatisticsServiceImpl(venueDao, statsDao, demandMetrics, CLOCK);

    var response = service.findOwned(OWNER_ID, null, "month", null, null);

    assertThat(response.period()).isEqualTo("month");
    assertThat(response.fromDate()).isEqualTo("2026-07-01");
    assertThat(response.toDate()).isEqualTo("2026-07-29");
    assertThat(response.reservationsCount()).isEqualTo(10);
    assertThat(response.confirmedCount()).isEqualTo(8);
    assertThat(response.cancelledCount()).isEqualTo(2);
    assertThat(response.noShowCount()).isEqualTo(3);
    assertThat(response.attendedCount()).isEqualTo(5);
    assertThat(response.occupancyRate()).isEqualByComparingTo("50.0");
    assertThat(response.reviewsCount()).isEqualTo(4);
    assertThat(response.incidentsCount()).isEqualTo(3);
    assertThat(response.averageRating()).isEqualByComparingTo("3.50");
    assertThat(response.series()).hasSize(2);
    verify(statsDao)
        .aggregateVenueRange(
            VENUE_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 29), "Europe/Madrid", NOW);
    verify(demandMetrics)
        .assemble(
            VENUE_ID,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 29),
            8,
            ZoneId.of("Europe/Madrid"));
  }

  @Test
  void resolvesTodayWeekYearAndValidCustomRange() {
    VenueDao venueDao = mock(VenueDao.class);
    StatsDailyVenueDao statsDao = mock(StatsDailyVenueDao.class);
    VenueEntity venue = new VenueEntity();
    venue.setId(VENUE_ID);
    when(venueDao.findCurrentByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(venue));
    when(statsDao.findRange(
            org.mockito.ArgumentMatchers.eq(VENUE_ID),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    var service = service(venueDao, statsDao);

    assertThat(service.findOwned(OWNER_ID, null, "today", null, null).fromDate())
        .isEqualTo("2026-07-29");
    assertThat(service.findOwned(OWNER_ID, null, "week", null, null).fromDate())
        .isEqualTo("2026-07-27");
    assertThat(service.findOwned(OWNER_ID, null, "year", null, null).fromDate())
        .isEqualTo("2026-01-01");
    var custom =
        service.findOwned(
            OWNER_ID, null, "custom", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
    assertThat(custom.fromDate()).isEqualTo("2026-06-01");
    assertThat(custom.toDate()).isEqualTo("2026-06-30");
    assertThat(custom.occupancyRate()).isEqualByComparingTo("0.0");
    assertThat(custom.averageRating()).isNull();
  }

  @Test
  void rejectsInvalidRangesBeforeResolvingVenue() {
    VenueDao venueDao = mock(VenueDao.class);
    StatsDailyVenueDao statsDao = mock(StatsDailyVenueDao.class);
    var service = service(venueDao, statsDao);

    assertThatThrownBy(() -> service.findOwned(OWNER_ID, null, "unknown", null, null))
        .isInstanceOf(VenueStatisticsFilterInvalidException.class);
    assertThatThrownBy(
            () ->
                service.findOwned(
                    OWNER_ID, null, "custom", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2)))
        .isInstanceOf(VenueStatisticsFilterInvalidException.class);
    assertThatThrownBy(
            () ->
                service.findOwned(
                    OWNER_ID, null, "custom", LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1)))
        .isInstanceOf(VenueStatisticsFilterInvalidException.class);
    assertThatThrownBy(
            () ->
                service.findOwned(
                    OWNER_ID, null, "today", LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29)))
        .isInstanceOf(VenueStatisticsFilterInvalidException.class);
    verifyNoInteractions(venueDao, statsDao);
  }

  @Test
  void hidesAbsentOwnedVenueBeforeAggregation() {
    VenueDao venueDao = mock(VenueDao.class);
    StatsDailyVenueDao statsDao = mock(StatsDailyVenueDao.class);
    when(venueDao.findCurrentByOwnerUserId(OWNER_ID)).thenReturn(Optional.empty());
    var service = service(venueDao, statsDao);

    assertThatThrownBy(() -> service.findOwned(OWNER_ID, null, "today", null, null))
        .isInstanceOf(VenueStatisticsNotFoundException.class);
    verifyNoInteractions(statsDao);
  }

  @Test
  void resolvesAnExplicitAccessibleVenueAndHidesAnInaccessibleOne() {
    VenueDao venueDao = mock(VenueDao.class);
    StatsDailyVenueDao statsDao = mock(StatsDailyVenueDao.class);
    VenueEntity venue = new VenueEntity();
    venue.setId(VENUE_ID);
    when(venueDao.findAccessibleById(OWNER_ID, VENUE_ID)).thenReturn(Optional.of(venue));
    when(statsDao.findRange(VENUE_ID, LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29)))
        .thenReturn(List.of());
    var service = service(venueDao, statsDao);

    assertThat(service.findOwned(OWNER_ID, VENUE_ID, "today", null, null).period())
        .isEqualTo("today");
    verify(venueDao).findAccessibleById(OWNER_ID, VENUE_ID);

    UUID inaccessibleVenueId = UUID.randomUUID();
    when(venueDao.findAccessibleById(OWNER_ID, inaccessibleVenueId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.findOwned(OWNER_ID, inaccessibleVenueId, "today", null, null))
        .isInstanceOf(VenueStatisticsNotFoundException.class);
  }

  private StatsDailyVenueEntity stats(
      LocalDate date,
      long reservations,
      long confirmed,
      long cancelled,
      long noShows,
      long attended,
      long occupied,
      long available,
      long reviews,
      long incidents,
      String average) {
    StatsDailyVenueEntity result = new StatsDailyVenueEntity();
    result.setDate(date);
    result.setReservationsCount(reservations);
    result.setConfirmedCount(confirmed);
    result.setCancelledCount(cancelled);
    result.setNoShowCount(noShows);
    result.setAttendedCount(attended);
    result.setOccupiedCapacity(occupied);
    result.setAvailableCapacity(available);
    result.setReviewsCount(reviews);
    result.setIncidentsCount(incidents);
    result.setAverageRating(new BigDecimal(average));
    return result;
  }

  private VenueStatisticsServiceImpl service(VenueDao venueDao, StatsDailyVenueDao statsDao) {
    DemandCommercialMetricsAssembler demandMetrics = mock(DemandCommercialMetricsAssembler.class);
    return new VenueStatisticsServiceImpl(venueDao, statsDao, demandMetrics, CLOCK);
  }
}
