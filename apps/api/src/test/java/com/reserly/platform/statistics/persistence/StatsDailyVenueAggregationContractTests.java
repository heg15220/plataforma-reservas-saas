package com.reserly.platform.statistics.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/** Protege la semántica de la consulta de agregación sin ejecutar infraestructura externa. */
class StatsDailyVenueAggregationContractTests {

  @Test
  void aggregationCoversRequiredMetricsAndUsesIdempotentUpsert() throws NoSuchMethodException {
    Method method =
        StatsDailyVenueDao.class.getMethod(
            "aggregateDate",
            java.time.LocalDate.class,
            java.time.Instant.class,
            java.time.Instant.class,
            java.time.Instant.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(query).isNotNull();
    assertThat(query.nativeQuery()).isTrue();
    assertThat(query.value())
        .contains("'cancelled_by_user', 'cancelled_by_venue'")
        .contains("'no_show', 'reported'")
        .contains("SUM(reservation.\"partySize\")")
        .contains("slot.\"status\" IN ('available', 'full')")
        .contains("ROUND(AVG(review.\"rating\"), 2)")
        .contains("review.\"createdAt\" >= :dayStart")
        .contains("review.\"createdAt\" < :dayEnd")
        .contains("ON CONFLICT (\"venueId\", \"date\") DO UPDATE");
  }

  @Test
  void ownedRangeAggregationIsBoundedAndCreatesZeroActivityDays() throws NoSuchMethodException {
    Method method =
        StatsDailyVenueDao.class.getMethod(
            "aggregateVenueRange",
            java.util.UUID.class,
            java.time.LocalDate.class,
            java.time.LocalDate.class,
            String.class,
            java.time.Instant.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(query).isNotNull();
    assertThat(query.nativeQuery()).isTrue();
    assertThat(query.value())
        .contains("generate_series(")
        .contains("reservation.\"venueId\" = :venueId")
        .contains("slot.\"venueId\" = :venueId")
        .contains("review.\"venueId\" = :venueId")
        .contains("AT TIME ZONE :zoneId")
        .contains("FROM dates")
        .contains("ON CONFLICT (\"venueId\", \"date\") DO UPDATE");
  }
}
