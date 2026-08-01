package com.reserly.platform.statistics.service;

import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consolida cada madrugada la jornada local anterior.
 *
 * <p>El servicio usa UPSERT, por lo que una repetición manual o un solapamiento entre nodos
 * converge en la misma instantánea. Los logs contienen solo fecha y número de locales.
 */
@Component
public class DailyVenueStatsAggregationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(DailyVenueStatsAggregationJob.class);

  private final DailyVenueStatsAggregationService aggregationService;
  private final Clock clock;

  public DailyVenueStatsAggregationJob(
      DailyVenueStatsAggregationService aggregationService, Clock clock) {
    this.aggregationService = aggregationService;
    this.clock = clock;
  }

  /**
   * Agrega la fecha anterior según el reloj de negocio.
   *
   * @return número de locales recalculados para facilitar observabilidad y tests
   */
  @Scheduled(
      cron = "${reserly.statistics.daily.cron:0 15 0 * * *}",
      zone = "${reserly.business-clock.zone-id:Europe/Madrid}")
  public int aggregatePreviousDay() {
    LocalDate statsDate = LocalDate.now(clock).minusDays(1);
    int aggregated = aggregationService.aggregate(statsDate);
    LOGGER.info("Aggregated daily venue statistics for {} venues on {}", aggregated, statsDate);
    return aggregated;
  }
}
