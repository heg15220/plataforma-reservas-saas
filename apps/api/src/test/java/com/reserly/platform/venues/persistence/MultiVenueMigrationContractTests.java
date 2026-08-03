package com.reserly.platform.venues.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Protege el contrato de migración que habilita cuentas multi-local y destinatarios privados. */
class MultiVenueMigrationContractTests {

  @Test
  void migrationDropsTheSingleVenueConstraintAndBackfillsNotificationEmails() throws Exception {
    String sql =
        new ClassPathResource("db/migration/V36__enable_multi_venue_notification_emails.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("DROP INDEX IF EXISTS \"uqVenuesOwnerCurrent\"")
        .contains("ADD COLUMN \"notificationEmail\" varchar(320)")
        .contains("lower(btrim(\"contactEmail\"))")
        .contains("ixVenuesOwnerStatusName");
  }

  @Test
  void panelCredentialMigrationConfinesOneUserToOneVenue() throws Exception {
    String sql =
        new ClassPathResource("db/migration/V37__create_venue_panel_credentials.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("CREATE TABLE \"VenuePanelCredentials\"")
        .contains("UNIQUE (\"venueId\")")
        .contains("UNIQUE (\"userId\")")
        .contains("REFERENCES \"Users\" (\"id\") ON DELETE CASCADE")
        .contains("REFERENCES \"Venues\" (\"id\") ON DELETE CASCADE");
  }

  @Test
  void capabilityMigrationDefaultsExistingAccountsToSingleVenue() throws Exception {
    String sql =
        new ClassPathResource(
                "db/migration/V38__restrict_additional_venues_to_multi_venue_accounts.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("\"multiVenueEnabled\" boolean NOT NULL DEFAULT false")
        .doesNotContain("DEFAULT true");
  }
}
