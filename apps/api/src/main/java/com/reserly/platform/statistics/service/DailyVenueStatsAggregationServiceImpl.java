package com.reserly.platform.statistics.service;

import com.reserly.platform.statistics.persistence.StatsDailyVenueDao;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Delimita una fecha en la zona IANA del reloj de negocio y delega una sola agregación atómica. */
@Service
public class DailyVenueStatsAggregationServiceImpl implements DailyVenueStatsAggregationService {

  private final StatsDailyVenueDao statsDao;
  private final Clock clock;

  public DailyVenueStatsAggregationServiceImpl(StatsDailyVenueDao statsDao, Clock clock) {
    this.statsDao = statsDao;
    this.clock = clock;
  }

  /**
   * {@inheritDoc}
   *
   * <p>El final se obtiene desde el inicio del día siguiente, no sumando veinticuatro horas, para
   * respetar cambios de horario de verano.
   */
  @Override
  @Transactional
  public int aggregate(LocalDate date) {
    if (date == null || date.isAfter(LocalDate.now(clock))) {
      throw new IllegalArgumentException("Stats date must not be null or future");
    }
    Instant dayStart = date.atStartOfDay(clock.getZone()).toInstant();
    Instant dayEnd = date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    Instant calculatedAt = clock.instant();
    return statsDao.aggregateDate(date, dayStart, dayEnd, calculatedAt);
  }
}
