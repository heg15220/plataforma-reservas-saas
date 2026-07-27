package com.reserly.platform.incidents.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Verifica el contrato físico completo de 10.1 sin depender de Docker o PostgreSQL. */
class IncidentSchemaPersistenceTests {

  @Test
  void migrationsDefineIncidentPenaltyAndBookingRuleTables() throws IOException {
    String incidentMigration = migration("/db/migration/V26__create_no_show_incidents.sql");
    String phaseMigration =
        migration("/db/migration/V27__create_penalties_and_venue_booking_rules.sql");

    assertThat(incidentMigration)
        .contains("CREATE TABLE \"NoShowIncidents\"")
        .contains("CONSTRAINT \"uqNoShowIncidentsReservation\"");
    assertThat(phaseMigration)
        .contains("CREATE TABLE \"Penalties\"")
        .contains("CONSTRAINT \"ckPenaltiesScopeVenue\"")
        .contains("CREATE UNIQUE INDEX \"uqPenaltiesActiveGlobalEmail\"")
        .contains("CREATE TABLE \"VenueBookingRules\"")
        .contains("CONSTRAINT \"uqVenueBookingRulesVenue\"")
        .contains("CONSTRAINT \"ckVenueBookingRulesCancellationMinutes\"")
        .contains("INSERT INTO \"VenueBookingRules\"")
        .contains("SELECT \"id\", true, \"cancellationNoticeMinutes\"");
  }

  @Test
  void entitiesMapUpperCamelTablesAndCriticalLowerCamelColumns() throws Exception {
    assertThat(PenaltyEntity.class.getAnnotation(Table.class).name()).isEqualTo("\"Penalties\"");
    assertThat(VenueBookingRuleEntity.class.getAnnotation(Table.class).name())
        .isEqualTo("\"VenueBookingRules\"");
    assertThat(
            PenaltyEntity.class
                .getMethod("getIncidentCountOperational")
                .getAnnotation(Column.class)
                .name())
        .isEqualTo("\"incidentCountOperational\"");
    assertThat(
            VenueBookingRuleEntity.class
                .getMethod("getFreeCancellationUntilMinutesBefore")
                .getAnnotation(Column.class)
                .name())
        .isEqualTo("\"freeCancellationUntilMinutesBefore\"");
  }

  private String migration(String path) throws IOException {
    try (InputStream input = getClass().getResourceAsStream(path)) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
