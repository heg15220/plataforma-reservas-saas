package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifica el ciclo de vida real del perfil y su alcance por propietario sobre PostgreSQL. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VenueProfileServiceIntegrationTests {

  private static final UUID RESTAURANT_CATEGORY_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID SPORTS_CENTER_CATEGORY_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000006");

  @Autowired private VenueProfileService venueProfileService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void createsReadsUpdatesArchivesAndRecreatesOnlyTheOwnersProfile() {
    UUID ownerUserId = createVenueOwner("profile-owner");
    UUID otherOwnerUserId = createVenueOwner("profile-other");

    VenueEntity created = venueProfileService.create(ownerUserId, initialCommand());

    assertThat(created.getId()).isNotNull();
    assertThat(created.getOwnerUser().getId()).isEqualTo(ownerUserId);
    assertThat(created.getBusinessAccount().getOwnerUser().getId()).isEqualTo(ownerUserId);
    assertThat(created.getCategory().getId()).isEqualTo(RESTAURANT_CATEGORY_ID);
    assertThat(created.getName()).isEqualTo("Café Central");
    assertThat(created.getSlug()).startsWith("cafe-central-");
    assertThat(created.getDescription()).isEqualTo("Cocina de mercado");
    assertThat(created.getDescriptionI18n().resolve(SupportedLocale.EN)).contains("Market cuisine");
    assertThat(created.getServicesI18n().resolve(SupportedLocale.EN))
        .contains("Reservas y eventos");
    assertThat(created.getRulesI18n().resolve(SupportedLocale.ES))
        .contains("Arrive ten minutes early");
    assertThat(created.getPublicTextI18n().resolve(null)).contains("Welcome");
    assertThat(created.getContactEmail()).isEqualTo("reservas@example.invalid");
    assertThat(created.getStatus()).isEqualTo("draft");
    assertThat(created.getManualAvailabilityStatus()).isEqualTo("automatic");
    assertThat(venueProfileService.find(ownerUserId).getId()).isEqualTo(created.getId());
    assertThatThrownBy(() -> venueProfileService.find(otherOwnerUserId))
        .isInstanceOf(VenueProfileNotFoundException.class);

    VenueEntity updated = venueProfileService.update(ownerUserId, updatedCommand());

    assertThat(updated.getId()).isEqualTo(created.getId());
    assertThat(updated.getSlug()).isEqualTo(created.getSlug());
    assertThat(updated.getStatus()).isEqualTo("draft");
    assertThat(updated.getCategory().getId()).isEqualTo(SPORTS_CENTER_CATEGORY_ID);
    assertThat(updated.getName()).isEqualTo("Centro Activo");
    assertThat(updated.getDescription()).isNull();
    assertThat(updated.getDescriptionI18n()).isNull();
    assertThat(updated.getServicesI18n()).isNull();
    assertThat(updated.getRulesI18n()).isNull();
    assertThat(updated.getPublicTextI18n()).isNull();
    assertThat(updated.getContactEmail()).isNull();
    assertThat(updated.getLatitude()).isEqualByComparingTo("40.416775");
    assertThat(updated.getLongitude()).isEqualByComparingTo("-3.703790");
    assertThat(updated.isShowPhone()).isTrue();
    assertThat(updated.isShowEmail()).isFalse();

    assertThatThrownBy(() -> venueProfileService.create(ownerUserId, initialCommand()))
        .isInstanceOf(VenueProfileConflictException.class);

    venueProfileService.archive(ownerUserId);

    assertThatThrownBy(() -> venueProfileService.find(ownerUserId))
        .isInstanceOf(VenueProfileNotFoundException.class);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT "status"
                FROM "Venues"
                WHERE "id" = ?
                """,
                String.class,
                created.getId()))
        .isEqualTo("archived");

    VenueEntity recreated = venueProfileService.create(ownerUserId, initialCommand());
    assertThat(recreated.getId()).isNotEqualTo(created.getId());
  }

  @Test
  void rejectsPartialCoordinatesAndUnknownCategoriesWithoutWriting() {
    UUID ownerUserId = createVenueOwner("profile-invalid");
    VenueProfileCommand partialCoordinates =
        new VenueProfileCommand(
            "Local inválido",
            RESTAURANT_CATEGORY_ID,
            null,
            null,
            null,
            null,
            "es",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new BigDecimal("40.416775"),
            null,
            false,
            false);
    VenueProfileCommand unknownCategory =
        new VenueProfileCommand(
            "Local inválido",
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            "es",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false);

    assertThatThrownBy(() -> venueProfileService.create(ownerUserId, partialCoordinates))
        .isInstanceOf(VenueProfileInvalidException.class);
    assertThatThrownBy(() -> venueProfileService.create(ownerUserId, unknownCategory))
        .isInstanceOf(VenueProfileInvalidException.class);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "Venues"
                WHERE "ownerUserId" = ?
                """,
                Integer.class,
                ownerUserId))
        .isZero();
  }

  private VenueProfileCommand initialCommand() {
    return new VenueProfileCommand(
        "  Café Central  ",
        RESTAURANT_CATEGORY_ID,
        localized("es", "Cocina de mercado", "Market cuisine"),
        new com.reserly.platform.localization.LocalizedText(
            SupportedLocale.ES, Map.of(SupportedLocale.ES, "Reservas y eventos")),
        new com.reserly.platform.localization.LocalizedText(
            SupportedLocale.EN, Map.of(SupportedLocale.EN, "Arrive ten minutes early")),
        localized("es", "Te damos la bienvenida", "Welcome"),
        "es",
        "  RESERVAS@EXAMPLE.INVALID  ",
        "  +34 910 000 000  ",
        "  Calle Mayor, 1  ",
        "  Madrid  ",
        "  Madrid  ",
        "ES",
        "  28013  ",
        null,
        null,
        true,
        true);
  }

  private VenueProfileCommand updatedCommand() {
    return new VenueProfileCommand(
        "Centro Activo",
        SPORTS_CENTER_CATEGORY_ID,
        null,
        null,
        null,
        null,
        "en",
        null,
        "+34 920 000 000",
        "Avenida Europa, 2",
        "Madrid",
        "Madrid",
        "ES",
        "28014",
        new BigDecimal("40.416775"),
        new BigDecimal("-3.703790"),
        true,
        false);
  }

  private com.reserly.platform.localization.LocalizedText localized(
      String sourceLocale, String spanish, String english) {
    return com.reserly.platform.localization.LocalizedText.fromLanguageTagValues(
        sourceLocale, Map.of("es", spanish, "en", english));
  }

  private UUID createVenueOwner(String prefix) {
    UUID userId = UUID.randomUUID();
    UUID businessAccountId = UUID.randomUUID();
    String email = prefix + "-" + userId + "@example.invalid";
    jdbcTemplate.update(
        """
        INSERT INTO "Users" (
          "id", "email", "emailNormalized", "passwordHash", "preferredLocale",
          "status", "accountType"
        ) VALUES (?, ?, ?, 'test-password-hash', 'es', 'active', 'venue_business')
        """,
        userId,
        email,
        email);
    jdbcTemplate.update(
        """
        INSERT INTO "BusinessAccounts" (
          "id", "ownerUserId", "taxCountry", "businessLegalName",
          "businessTaxIdentifier", "businessTaxIdentifierNormalized"
        ) VALUES (?, ?, 'ES', 'Negocio de prueba', ?, ?)
        """,
        businessAccountId,
        userId,
        "B" + userId.toString().substring(0, 8),
        "B" + userId.toString().substring(0, 8));
    return userId;
  }
}
