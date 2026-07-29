package com.reserly.platform.statistics.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Verifica rápidamente el contrato SQL de estadísticas sin levantar PostgreSQL ni Docker. */
class StatsDailyVenueMigrationTests {

  @Test
  void migrationDefinesDailyUniquenessMetricConstraintsAndIndexes() throws IOException {
    try (var input =
        getClass().getResourceAsStream("/db/migration/V31__create_daily_venue_stats.sql")) {
      assertThat(input).isNotNull();
      String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

      assertThat(sql)
          .contains("CREATE TABLE \"StatsDailyVenue\"")
          .contains("CONSTRAINT \"uqStatsDailyVenueVenueDate\" UNIQUE (\"venueId\", \"date\")")
          .contains("CONSTRAINT \"ckStatsDailyVenueCounts\"")
          .contains("CONSTRAINT \"ckStatsDailyVenueRating\"")
          .contains("FOREIGN KEY (\"venueId\") REFERENCES \"Venues\" (\"id\") ON DELETE CASCADE")
          .contains("CREATE INDEX \"ixStatsDailyVenueDateVenue\"");
    }
  }
}
