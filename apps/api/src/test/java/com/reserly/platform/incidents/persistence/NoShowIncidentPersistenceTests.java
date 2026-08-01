package com.reserly.platform.incidents.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Verifica el contrato físico y el mapeo de la fuente del historial sin arrancar PostgreSQL. */
class NoShowIncidentPersistenceTests {

  @Test
  void migrationDefinesOwnershipAuditConstraintsAndEmailHistoryIndex() throws IOException {
    String migration = migration();

    assertThat(migration)
        .contains("CREATE TABLE \"NoShowIncidents\"")
        .contains("CONSTRAINT \"fkNoShowIncidentsVenue\"")
        .contains("CONSTRAINT \"fkNoShowIncidentsReservation\"")
        .contains("CONSTRAINT \"fkNoShowIncidentsReportedByUser\"")
        .contains("CONSTRAINT \"uqNoShowIncidentsReservation\"")
        .contains("CONSTRAINT \"ckNoShowIncidentsEmailNormalized\"")
        .contains("CONSTRAINT \"ckNoShowIncidentsType\"")
        .contains("CONSTRAINT \"ckNoShowIncidentsStatus\"")
        .contains("CREATE INDEX \"ixNoShowIncidentsEmailReportedAt\"");
  }

  @Test
  void entityMapsUpperCamelTableAndLowerCamelColumns() throws Exception {
    assertThat(NoShowIncidentEntity.class.getAnnotation(Table.class).name())
        .isEqualTo("\"NoShowIncidents\"");
    assertThat(
            NoShowIncidentEntity.class
                .getMethod("getCustomerEmailNormalized")
                .getAnnotation(Column.class)
                .name())
        .isEqualTo("\"customerEmailNormalized\"");
    assertThat(
            NoShowIncidentEntity.class
                .getMethod("getReportedByUserId")
                .getAnnotation(Column.class)
                .name())
        .isEqualTo("\"reportedByUserId\"");
  }

  private String migration() throws IOException {
    try (InputStream input =
        getClass().getResourceAsStream("/db/migration/V26__create_no_show_incidents.sql")) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
