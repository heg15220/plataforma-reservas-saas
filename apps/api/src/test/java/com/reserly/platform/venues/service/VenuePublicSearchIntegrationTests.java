package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.persistence.VenueEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifica la búsqueda pública textual contra PostgreSQL real y locales publicados. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VenuePublicSearchIntegrationTests {

  private static final UUID RESTAURANT_CATEGORY_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID PADEL_CATEGORY_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000004");

  @Autowired private VenueProfileService venueProfileService;
  @Autowired private VenuePublicationService venuePublicationService;
  @Autowired private VenuePublicSearchService searchService;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void searchesPublishedVenuesByAccentInsensitiveNameAndPublicKeywords() {
    UUID cafeOwnerId = createVenueOwner("search-cafe");
    UUID padelOwnerId = createVenueOwner("search-padel");
    preparePublicationEligibility(cafeOwnerId);
    preparePublicationEligibility(padelOwnerId);
    createPublishableVenue(
        cafeOwnerId,
        "Café Central",
        RESTAURANT_CATEGORY_ID,
        "Cocina de mercado con café de especialidad",
        "Market cuisine with specialty coffee");
    createPublishableVenue(
        padelOwnerId,
        "Pista Norte",
        PADEL_CATEGORY_ID,
        "Pádel cubierto para partidos rápidos",
        "Indoor padel for quick matches");
    venuePublicationService.publish(cafeOwnerId);
    venuePublicationService.publish(padelOwnerId);
    entityManager.flush();
    entityManager.clear();

    var byName = searchService.search(SupportedLocale.ES, "cafe", 0, 20);
    var byKeyword = searchService.search(SupportedLocale.ES, "padel", 0, 20);

    assertThat(byName.results()).extracting("name").containsExactly("Café Central");
    assertThat(byKeyword.results()).extracting("name").containsExactly("Pista Norte");
  }

  private VenueEntity createPublishableVenue(
      UUID ownerUserId,
      String name,
      UUID categoryId,
      String spanishDescription,
      String englishDescription) {
    VenueEntity venue =
        venueProfileService.create(
            ownerUserId,
            publishableCommand(name, categoryId, spanishDescription, englishDescription));
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

  private VenueProfileCommand publishableCommand(
      String name, UUID categoryId, String spanishDescription, String englishDescription) {
    return new VenueProfileCommand(
        name,
        categoryId,
        localized(spanishDescription, englishDescription),
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

  private LocalizedText localized(String spanish, String english) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, spanish, SupportedLocale.EN, english));
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

  private void preparePublicationEligibility(UUID ownerUserId) {
    Instant verifiedAt = Instant.now().minusSeconds(60);
    jdbcTemplate.update(
        """
        UPDATE "Users"
        SET "emailVerifiedAt" = ?, "status" = 'active'
        WHERE "id" = ?
        """,
        Timestamp.from(verifiedAt),
        ownerUserId);
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
