package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueCustomTabCommand;
import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifica la exposición pública real de pestañas personalizadas sobre PostgreSQL. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VenueCustomTabPublicationIntegrationTests {

  private static final UUID RESTAURANT_CATEGORY_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");

  @Autowired private VenueProfileService venueProfileService;
  @Autowired private VenuePublicationService venuePublicationService;
  @Autowired private VenueCustomTabService customTabService;
  @Autowired private VenuePublicProfileService publicProfileService;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void exposesOnlyActiveTabsFromPublishedVenuesOrderedAndLocalized() {
    UUID ownerUserId = createVenueOwner("custom-tabs-public");
    markEmailVerified(ownerUserId);
    markBusinessVerificationStatus(ownerUserId);
    VenueEntity venue = createPublishableVenue(ownerUserId, "Casa Tabs");

    VenueCustomTabEntity menu =
        customTabService.create(
            ownerUserId,
            activeTab(
                "Carta",
                "Menu",
                "<p onclick=\"alert(1)\">Menu del dia</p><script>alert(1)</script>"
                    + "<a href=\"javascript:alert(1)\">Enlace</a>",
                "<p>Daily menu</p><strong>Season</strong>"));
    VenueCustomTabEntity prices =
        customTabService.create(
            ownerUserId,
            activeTab("Precios", "Prices", "<p>Desde 12 EUR</p>", "<p>From 12 EUR</p>"));
    VenueCustomTabEntity draft =
        customTabService.create(
            ownerUserId,
            new VenueCustomTabCommand(
                localized("Borrador", "Draft"),
                localized("<p>Oculto</p>", "<p>Hidden</p>"),
                false));
    customTabService.reorder(ownerUserId, List.of(prices.getId(), menu.getId(), draft.getId()));
    venuePublicationService.publish(ownerUserId);
    entityManager.flush();
    entityManager.clear();

    var english = publicProfileService.findBySlug(venue.getSlug(), SupportedLocale.EN);
    var spanish = publicProfileService.findBySlug(venue.getSlug(), SupportedLocale.ES);

    assertThat(english.customTabs()).hasSize(2);
    assertThat(english.customTabs()).extracting("title").containsExactly("Prices", "Menu");
    assertThat(english.customTabs()).extracting("position").containsExactly(0, 1);
    assertThat(english.customTabs()).extracting("contentFormat").containsOnly("safe_html");
    assertThat(english.customTabs().get(1).content()).contains("Daily menu", "Season");
    assertThat(english.customTabs().get(1).content())
        .doesNotContain("<script", "onclick", "javascript:");

    assertThat(spanish.customTabs()).extracting("title").containsExactly("Precios", "Carta");
    assertThat(spanish.customTabs().get(1).content()).contains("Menu del dia", "Enlace");
    assertThat(spanish.customTabs().get(1).content())
        .doesNotContain("<script", "onclick", "javascript:");
  }

  @Test
  void doesNotExposeTabsWhenTheVenueIsStillDraft() {
    UUID ownerUserId = createVenueOwner("custom-tabs-draft");
    VenueEntity venue = createPublishableVenue(ownerUserId, "Casa Borrador");
    customTabService.create(
        ownerUserId, activeTab("Carta privada", "Private menu", "<p>Oculto</p>", "<p>Hidden</p>"));
    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(() -> publicProfileService.findBySlug(venue.getSlug(), SupportedLocale.ES))
        .isInstanceOf(VenueProfileNotFoundException.class);
  }

  private VenueCustomTabCommand activeTab(
      String titleEs, String titleEn, String contentEs, String contentEn) {
    return new VenueCustomTabCommand(
        localized(titleEs, titleEn), localized(contentEs, contentEn), true);
  }

  private LocalizedText localized(String spanish, String english) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, spanish, SupportedLocale.EN, english));
  }

  private VenueEntity createPublishableVenue(UUID ownerUserId, String name) {
    VenueEntity venue = venueProfileService.create(ownerUserId, publishableCommand(name));
    jdbcTemplate.update(
        """
        UPDATE "Venues"
        SET "mainImageUrl" = ?,
            "mainImageObjectKey" = 'venues/test/main.png',
            "mainImageMediaType" = 'image/png',
            "mainImageSizeBytes" = 1024,
            "mainImageWidth" = 640,
            "mainImageHeight" = 480
        WHERE "id" = ?
        """,
        "/api/public/venue-images/" + venue.getId() + "/main",
        venue.getId());
    entityManager.refresh(venue);
    return venue;
  }

  private VenueProfileCommand publishableCommand(String name) {
    return new VenueProfileCommand(
        name,
        RESTAURANT_CATEGORY_ID,
        localized("Descripción completa", "Complete description"),
        null,
        null,
        null,
        "es",
        "contacto@example.invalid",
        null,
        "Calle Mayor, 1",
        "Madrid",
        "Madrid",
        "ES",
        "28013",
        new BigDecimal("40.416775"),
        new BigDecimal("-3.703790"),
        false,
        true);
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

  private void markEmailVerified(UUID ownerUserId) {
    jdbcTemplate.update(
        """
        UPDATE "Users"
        SET "emailVerifiedAt" = ?, "status" = 'active'
        WHERE "id" = ?
        """,
        Timestamp.from(Instant.now().minusSeconds(60)),
        ownerUserId);
  }

  private void markBusinessVerificationStatus(UUID ownerUserId) {
    Instant verifiedAt = Instant.now().minusSeconds(60);
    jdbcTemplate.update(
        """
        UPDATE "BusinessAccounts"
        SET "businessVerificationStatus" = 'verified',
            "activeVerificationRequestId" = NULL,
            "businessVerifiedAt" = ?,
            "businessVerificationExpiresAt" = ?,
            "manualReviewStatus" = NULL,
            "manualReviewedByUserId" = NULL,
            "manualReviewedAt" = NULL
        WHERE "ownerUserId" = ?
        """,
        Timestamp.from(verifiedAt),
        Timestamp.from(verifiedAt.plusSeconds(86_400)),
        ownerUserId);
  }
}
