package com.reserly.platform.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.statistics.persistence.StatsDailyVenueDao;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Verifica fronteras locales, DST y rechazo de fechas inválidas sin arrancar Spring. */
class DailyVenueStatsAggregationServiceTests {

  private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
  private static final Instant NOW = Instant.parse("2026-03-30T10:00:00Z");
  private static final LocalDate DATE = LocalDate.of(2026, 3, 29);

  @Test
  void aggregatesOneLocalDateUsingDstSafeInstantBoundaries() {
    StatsDailyVenueDao statsDao = mock(StatsDailyVenueDao.class);
    Clock clock = Clock.fixed(NOW, ZONE);
    when(statsDao.aggregateDate(
            DATE,
            Instant.parse("2026-03-28T23:00:00Z"),
            Instant.parse("2026-03-29T22:00:00Z"),
            NOW))
        .thenReturn(7);
    var service = new DailyVenueStatsAggregationServiceImpl(statsDao, clock);

    int result = service.aggregate(DATE);

    assertThat(result).isEqualTo(7);
    verify(statsDao)
        .aggregateDate(
            DATE,
            Instant.parse("2026-03-28T23:00:00Z"),
            Instant.parse("2026-03-29T22:00:00Z"),
            NOW);
  }

  @Test
  void rejectsNullAndFutureDatesBeforePersistence() {
    StatsDailyVenueDao statsDao = mock(StatsDailyVenueDao.class);
    var service = new DailyVenueStatsAggregationServiceImpl(statsDao, Clock.fixed(NOW, ZONE));

    assertThatThrownBy(() -> service.aggregate(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.aggregate(LocalDate.of(2026, 3, 31)))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(statsDao);
  }
}
