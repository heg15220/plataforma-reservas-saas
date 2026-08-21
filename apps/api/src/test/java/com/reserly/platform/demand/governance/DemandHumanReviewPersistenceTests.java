package com.reserly.platform.demand.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Verifica restricciones físicas y serialización sin depender de convenciones de implementación.
 */
class DemandHumanReviewPersistenceTests {
  @Test
  void migrationConstrainsScopeStatusAndVenueAppealOwnership() throws Exception {
    String sql;
    try (var stream =
        getClass().getResourceAsStream("/db/migration/V62__create_demand_governance_reviews.sql")) {
      assertThat(stream).isNotNull();
      sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertThat(sql)
        .contains("CREATE TABLE \"DemandGovernanceReviews\"")
        .contains("'attribute', 'commercial_decision'")
        .contains(
            "'submitted', 'approved', 'rejected', 'correction_requested', 'corrected', 'appealed'")
        .contains("\"reviewType\" = 'commercial_decision' AND \"venueId\" IS NOT NULL");
  }

  @Test
  void daoUsesTransactionScopedLockForFirstSubmission() throws Exception {
    Query query =
        DemandHumanReviewDao.class
            .getMethod("lockSubmission", UUID.class)
            .getAnnotation(Query.class);
    assertThat(query.nativeQuery()).isTrue();
    assertThat(query.value()).contains("pg_advisory_xact_lock", ":id");
  }
}
