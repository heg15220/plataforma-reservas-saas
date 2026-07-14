package com.reserly.platform.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica exclusivamente el contrato fisico de formularios sobre PostgreSQL/PostGIS efimero.
 *
 * <p>V23 conserva el snapshot y convierte reservationId en una referencia física protegida.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservationFormMigrationIntegrationTests {

  @Autowired private Flyway flyway;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void createsLocalizedFormContractAtVersionTwentyThree() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("23");
    assertThat(columns("ReservationFormFields"))
        .containsExactly(
            "id",
            "venueId",
            "label",
            "labelI18n",
            "key",
            "type",
            "isRequired",
            "optionsJson",
            "optionsI18nJson",
            "position",
            "isActive",
            "createdAt",
            "updatedAt");
    assertThat(columns("Venues"))
        .contains(
            "reservationFormPublished",
            "reservationFormFallbackApproved",
            "reservationFormPublishedAt");
    assertThat(columns("ReservationFormResponses"))
        .containsExactly(
            "id",
            "reservationId",
            "fieldId",
            "fieldKey",
            "fieldLabel",
            "valueJson",
            "createdAt");
  }

  @Test
  void createsOwnershipHistoryConstraintsAndIndexes() {
    assertThat(constraints("ReservationFormFields"))
        .contains(
            "fkReservationFormFieldsVenue",
            "uqReservationFormFieldsVenueKey",
            "ckReservationFormFieldsKey",
            "ckReservationFormFieldsType",
            "ckReservationFormFieldsOptions",
            "ckReservationFormFieldsLabelI18nObject",
            "ckReservationFormFieldsOptionsI18nArray");
    assertThat(constraints("ReservationFormResponses"))
        .contains(
            "fkReservationFormResponsesField",
            "fkReservationFormResponsesReservation",
            "uqReservationFormResponsesReservationKey",
            "ckReservationFormResponsesFieldKey",
            "ckReservationFormResponsesFieldLabel");
    assertThat(indexes("ReservationFormFields"))
        .contains("ixReservationFormFieldsVenueActivePosition");
    assertThat(indexes("ReservationFormResponses"))
        .contains("ixReservationFormResponsesReservation", "ixReservationFormResponsesField");

    Integer reservationForeignKeys =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM "information_schema"."table_constraints" constraint_definition
            JOIN "information_schema"."key_column_usage" key_column
              ON constraint_definition."constraint_name" = key_column."constraint_name"
             AND constraint_definition."constraint_schema" = key_column."constraint_schema"
            WHERE constraint_definition."table_schema" = current_schema()
              AND constraint_definition."table_name" = 'ReservationFormResponses'
              AND constraint_definition."constraint_type" = 'FOREIGN KEY'
              AND key_column."column_name" = 'reservationId'
            """,
            Integer.class);
    assertThat(reservationForeignKeys).isOne();
  }

  private List<String> columns(String table) {
    return jdbcTemplate.queryForList(
        """
        SELECT "column_name"
        FROM "information_schema"."columns"
        WHERE "table_schema" = current_schema()
          AND "table_name" = ?
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
        WHERE "table_schema" = current_schema()
          AND "table_name" = ?
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
        WHERE "schemaname" = current_schema()
          AND "tablename" = ?
        ORDER BY "indexname"
        """,
        String.class,
        table);
  }
}
