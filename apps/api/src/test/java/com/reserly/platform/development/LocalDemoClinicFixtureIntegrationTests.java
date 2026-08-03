package com.reserly.platform.development;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

/**
 * Ejecuta el fixture local sobre PostgreSQL real para proteger el recorrido clínico completo.
 *
 * <p>La prueba aplica dos veces el SQL, como ocurriría tras reiniciar la API local, y comprueba que
 * la publicación, sus especialidades, profesionales, asociaciones y citas exactas permanecen
 * idempotentes y consultables.
 */
@SpringBootTest
@ActiveProfiles("test")
class LocalDemoClinicFixtureIntegrationTests {

  private static final String CLINIC_ID = "e3000000-0000-4000-8000-000000000001";

  @Autowired private DataSource dataSource;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void publishesClinicProfessionalsAndFutureExactTimeAppointmentsIdempotently() {
    ResourceDatabasePopulator fixture =
        new ResourceDatabasePopulator(new ClassPathResource("dev-fixtures/local-demo-venues.sql"));

    fixture.execute(dataSource);
    fixture.execute(dataSource);

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "Venues"
                WHERE "id" = ?::uuid
                  AND "slug" = 'clinica-alba-integral'
                  AND "status" = 'published'
                  AND "reservationFormPublished" = true
                """,
                Integer.class,
                CLINIC_ID))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "Services"
                WHERE "venueId" = ?::uuid
                  AND "bookingMode" = 'exact_time'
                  AND "isActive" = true
                """,
                Integer.class,
                CLINIC_ID))
        .isEqualTo(3);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "EmployeeResources"
                WHERE "venueId" = ?::uuid
                  AND "type" = 'professional'
                  AND "status" = 'active'
                  AND "publicVisibility" = true
                """,
                Integer.class,
                CLINIC_ID))
        .isEqualTo(4);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "ServiceEmployeeResources" AS association
                JOIN "Services" AS service ON service."id" = association."serviceId"
                WHERE service."venueId" = ?::uuid
                """,
                Integer.class,
                CLINIC_ID))
        .isEqualTo(4);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "TimeSlots"
                WHERE "venueId" = ?::uuid
                  AND "date" > CURRENT_DATE
                  AND "status" = 'available'
                """,
                Integer.class,
                CLINIC_ID))
        .isGreaterThan(0);
  }
}
