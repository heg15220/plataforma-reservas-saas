package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.reviews.dto.PublicReviewCollectionResponse;
import com.reserly.platform.reviews.dto.ReviewItemResponse;
import com.reserly.platform.reviews.service.ReviewQueryService;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueCustomTabDao;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.persistence.VenueImageDao;
import com.reserly.platform.venues.persistence.VenueImageEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifica localización, privacidad y ausencia de perfiles no publicados en la proyección pública.
 */
@ExtendWith(MockitoExtension.class)
class VenuePublicProfileServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueImageDao imageDao;
  @Mock private VenueCustomTabDao customTabDao;
  @Mock private ReviewQueryService reviewQueryService;

  private VenuePublicProfileServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new VenuePublicProfileServiceImpl(venueDao, imageDao, customTabDao, reviewQueryService);
  }

  @Test
  void resolvesLocalizedTextsAndOnlyVisibleContactData() {
    VenueEntity venue = publishedVenue();
    VenueImageEntity second = image(venue, "/images/second", "Segunda", 1);
    VenueImageEntity first = image(venue, "/images/first", "Primera", 0);
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(imageDao.findAllPublishedByVenueId(venue.getId())).thenReturn(List.of(first, second));
    when(customTabDao.findAllPublishedActiveByVenueId(venue.getId()))
        .thenReturn(List.of(tab(venue, 0, "Carta", "Menu", "<p>Menú</p>", "<p>Menu</p>")));
    when(reviewQueryService.findPublic(venue.getId()))
        .thenReturn(
            new PublicReviewCollectionResponse(
                new BigDecimal("4.5"),
                2,
                false,
                List.of(
                    new ReviewItemResponse(
                        UUID.randomUUID(),
                        5,
                        "Excelente",
                        Instant.parse("2026-07-28T10:00:00Z")))));

    var response = service.findBySlug("casa-luz", SupportedLocale.EN);

    assertThat(response.locale()).isEqualTo("en");
    assertThat(response.categoryName()).isEqualTo("Restaurant");
    assertThat(response.description()).isEqualTo("Seasonal cuisine");
    assertThat(response.phone()).isNull();
    assertThat(response.contactEmail()).isEqualTo("hola@casaluz.test");
    assertThat(response.gallery()).extracting("position").containsExactly(0, 1);
    assertThat(response.customTabs()).extracting("title").containsExactly("Menu");
    assertThat(response.customTabs()).extracting("content").containsExactly("<p>Menu</p>");
    assertThat(response.reviews().averageRating()).isEqualByComparingTo("4.5");
    assertThat(response.reviews().reviewsCount()).isEqualTo(2);
    assertThat(response.reviews().items()).hasSize(1);
  }

  @Test
  void fallsBackToEnglishAndCanonicalValuesWithoutLeakingHiddenContacts() {
    VenueEntity venue = publishedVenue();
    venue.getCategory().setNameI18n(null);
    venue.setDescriptionI18n(
        new LocalizedText(SupportedLocale.EN, Map.of(SupportedLocale.EN, "English fallback")));
    venue.setShowEmail(false);
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(imageDao.findAllPublishedByVenueId(venue.getId())).thenReturn(List.of());
    when(customTabDao.findAllPublishedActiveByVenueId(venue.getId())).thenReturn(List.of());
    when(reviewQueryService.findPublic(venue.getId()))
        .thenReturn(new PublicReviewCollectionResponse(null, 0, false, List.of()));

    var response = service.findBySlug("casa-luz", SupportedLocale.ES);

    assertThat(response.categoryName()).isEqualTo("Restaurante");
    assertThat(response.description()).isEqualTo("English fallback");
    assertThat(response.contactEmail()).isNull();
  }

  @Test
  void treatsMissingOrUnpublishedSlugAsNotFound() {
    when(venueDao.findPublishedBySlug("borrador")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findBySlug("borrador", SupportedLocale.ES))
        .isInstanceOf(VenueProfileNotFoundException.class);

    verifyNoInteractions(customTabDao, reviewQueryService);
  }

  private static VenueEntity publishedVenue() {
    CategoryEntity category = new CategoryEntity();
    category.setName("Restaurante");
    category.setSlug("restaurante");
    category.setNameI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, "Restaurante", SupportedLocale.EN, "Restaurant")));
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setSlug("casa-luz");
    venue.setName("Casa Luz");
    venue.setCategory(category);
    venue.setDescription("Cocina de temporada");
    venue.setDescriptionI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(
                SupportedLocale.ES, "Cocina de temporada",
                SupportedLocale.EN, "Seasonal cuisine")));
    venue.setAddress("Calle Mayor, 1");
    venue.setCity("Madrid");
    venue.setCountry("ES");
    venue.setLatitude(new BigDecimal("40.416775"));
    venue.setLongitude(new BigDecimal("-3.703790"));
    venue.setPhone("+34910000000");
    venue.setContactEmail("hola@casaluz.test");
    venue.setShowPhone(false);
    venue.setShowEmail(true);
    return venue;
  }

  private static VenueImageEntity image(
      VenueEntity venue, String url, String altText, int position) {
    VenueImageEntity image = new VenueImageEntity();
    image.setVenue(venue);
    image.setUrl(url);
    image.setAltText(altText);
    image.setPosition(position);
    return image;
  }

  private static VenueCustomTabEntity tab(
      VenueEntity venue,
      int position,
      String titleEs,
      String titleEn,
      String contentEs,
      String contentEn) {
    VenueCustomTabEntity tab = new VenueCustomTabEntity();
    tab.setVenue(venue);
    tab.setPosition(position);
    tab.setActive(true);
    tab.setContentFormat("safe_html");
    tab.setTitleI18n(
        new LocalizedText(
            SupportedLocale.ES, Map.of(SupportedLocale.ES, titleEs, SupportedLocale.EN, titleEn)));
    tab.setContentI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, contentEs, SupportedLocale.EN, contentEn)));
    return tab;
  }
}
