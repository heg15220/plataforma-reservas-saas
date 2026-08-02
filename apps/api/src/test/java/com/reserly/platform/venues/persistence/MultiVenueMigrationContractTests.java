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
}
