package com.reserly.platform.demand.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/** Verifica inmutabilidad e idempotencia físicas sin depender de una convención JPA. */
class DemandGovernanceAuditPersistenceTests {

  @Test
  void migrationMakesAuditLogsAppendOnlyAndCoversSevenFamilies() throws IOException {
    String migration;
    try (var stream =
        getClass().getResourceAsStream("/db/migration/V61__harden_demand_governance_audit.sql")) {
      assertThat(stream).isNotNull();
      migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertThat(migration)
        .contains("BEFORE UPDATE OR DELETE ON \"AuditLogs\"")
        .contains("prevent_audit_log_mutation")
        .contains("CREATE UNIQUE INDEX \"uxAuditLogsDemandGovernanceEvent\"")
        .contains(
            "'demand_ontology'",
            "'demand_ranking_weights'",
            "'demand_model'",
            "'demand_experiment'",
            "'demand_promotion'",
            "'demand_waitlist'",
            "'demand_automatic_action'");
  }

  @Test
  void daoSerializesConcurrentRetriesWithTransactionScopedLock() throws Exception {
    Query query =
        com.reserly.platform.administration.persistence.AuditLogDao.class
            .getMethod("lockDemandGovernanceEvent", UUID.class)
            .getAnnotation(Query.class);

    assertThat(query.nativeQuery()).isTrue();
    assertThat(query.value()).contains("pg_advisory_xact_lock", ":eventId");
  }
}
