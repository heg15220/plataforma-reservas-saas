package com.reserly.platform.reservations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Verifica exclusivamente el contrato físico V23 sobre PostgreSQL/PostGIS efímero. */
@SpringBootTest
@ActiveProfiles("test")
class ReservationMigrationIntegrationTests {

  @Autowired private Flyway flyway;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void createsReservationColumnsAndIndexesAtVersionTwentyThree() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("23");
    assertThat(columns("Reservations"))
        .containsExactly(
            "id",
            "venueId",
            "timeSlotId",
            "serviceId",
            "employeeResourceId",
            "customerName",
            "customerEmail",
            "customerEmailNormalized",
            "partySize",
            "date",
            "startsAt",
            "endsAt",
            "status",
            "holdExpiresAt",
            "holdTokenHash",
            "secureTokenHash",
            "secureTokenExpiresAt",
            "cancelledAt",
            "cancelledBy",
            "cancellationReason",
            "attendanceMarkedAt",
            "createdAt",
            "updatedAt");
    assertThat(indexes("Reservations"))
        .contains(
            "ixReservationsVenueDate",
            "ixReservationsCustomerEmailNormalized",
            "ixReservationsStatusHoldExpiresAt",
            "ixReservationsTimeSlotStatus",
            "uqReservationsHoldTokenHash",
            "uqReservationsSecureTokenHash");
  }

  @Test
  void enforcesReservationRelationsAndFormResponseOwnership() {
    assertThat(constraints("Reservations"))
        .contains(
            "fkReservationsVenue",
            "fkReservationsTimeSlot",
            "fkReservationsService",
            "fkReservationsEmployeeResource",
            "ckReservationsPartySize",
            "ckReservationsStatus",
            "ckReservationsHoldState");
    assertThat(constraints("ReservationFormResponses"))
        .contains("fkReservationFormResponsesReservation");
  }

  private List<String> columns(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT "column_name"
        FROM "information_schema"."columns"
        WHERE "table_schema" = current_schema() AND "table_name" = ?
        ORDER BY "ordinal_position"
        """,
        String.class,
        table);
  }

  private List<String> constraints(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT "constraint_name"
        FROM "information_schema"."table_constraints"
        WHERE "table_schema" = current_schema() AND "table_name" = ?
        ORDER BY "constraint_name"
        """,
        String.class,
        table);
  }

  private List<String> indexes(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT "indexname"
        FROM "pg_indexes"
        WHERE "schemaname" = current_schema() AND "tablename" = ?
        ORDER BY "indexname"
        """,
        String.class,
        table);
  }
}
