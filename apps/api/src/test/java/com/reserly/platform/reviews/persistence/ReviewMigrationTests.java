package com.reserly.platform.reviews.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Verificación rápida del contrato SQL de reseñas sin arrancar contenedores ni toda la aplicación.
 */
class ReviewMigrationTests {

  @Test
  void migrationDefinesReviewConstraintsAndQueryIndexes() throws IOException {
    try (var input = getClass().getResourceAsStream("/db/migration/V30__create_reviews.sql")) {
      assertThat(input).isNotNull();
      String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

      assertThat(sql)
          .contains("CREATE TABLE \"Reviews\"")
          .contains("CONSTRAINT \"uqReviewsReservation\" UNIQUE (\"reservationId\")")
          .contains("CONSTRAINT \"ckReviewsRating\" CHECK (\"rating\" BETWEEN 1 AND 5)")
          .contains("CONSTRAINT \"ckReviewsEmailNormalized\"")
          .contains("CONSTRAINT \"fkReviewsReservationVenue\"")
          .contains("REFERENCES \"Reservations\" (\"id\", \"venueId\")")
          .contains("CREATE INDEX \"ixReviewsVenueCreatedAt\"")
          .contains("CREATE INDEX \"ixReviewsVenueCustomerEmail\"");
    }
  }
}
