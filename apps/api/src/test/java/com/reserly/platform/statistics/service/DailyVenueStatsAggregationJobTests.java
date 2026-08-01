package com.reserly.platform.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/** Verifica fecha local y planificación del job sin activar el scheduler. */
class DailyVenueStatsAggregationJobTests {

  @Test
  void aggregatesPreviousBusinessDayOnce() {
    DailyVenueStatsAggregationService service = mock(DailyVenueStatsAggregationService.class);
    LocalDate previousDay = LocalDate.of(2026, 7, 28);
    when(service.aggregate(previousDay)).thenReturn(5);
    Clock clock = Clock.fixed(Instant.parse("2026-07-29T00:30:00Z"), ZoneId.of("Europe/Madrid"));
    var job = new DailyVenueStatsAggregationJob(service, clock);

    int result = job.aggregatePreviousDay();

    assertThat(result).isEqualTo(5);
    verify(service).aggregate(previousDay);
  }

  @Test
  void declaresOneBoundedDailySchedule() throws NoSuchMethodException {
    Scheduled scheduled =
        DailyVenueStatsAggregationJob.class
            .getMethod("aggregatePreviousDay")
            .getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.cron()).isEqualTo("${reserly.statistics.daily.cron:0 15 0 * * *}");
    assertThat(scheduled.zone()).isEqualTo("${reserly.business-clock.zone-id:Europe/Madrid}");
  }
}
