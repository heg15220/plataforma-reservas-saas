package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica la proyección pública base de resultados de búsqueda. */
@ExtendWith(MockitoExtension.class)
class VenuePublicSearchServiceTests {

  @Mock private VenueDao venueDao;

  private VenuePublicSearchServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new VenuePublicSearchServiceImpl(venueDao);
  }

  @Test
  void listsOnlyPublishedProjectionProvidedByDaoWithLocalizedCardFields() {
    VenueEntity venue = venue("casa-luz", "Casa Luz");
    stubAdvancedSearch(List.of(venue), 1L);

    var response =
        service.search(SupportedLocale.EN, null, null, null, null, null, null, null, 0, 20);

    assertThat(response.locale()).isEqualTo("en");
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).slug()).isEqualTo("casa-luz");
    assertThat(response.results().get(0).categoryName()).isEqualTo("Restaurant");
    assertThat(response.results().get(0).descriptionExcerpt()).isEqualTo("Seasonal cuisine");
    assertThat(response.results().get(0).mainImageUrl())
        .isEqualTo("/api/public/venue-images/id/main");
    assertThat(response.results().get(0).city()).isEqualTo("Madrid");
    assertThat(response.results().get(0).statusCode()).isEqualTo("availability_pending");
    assertThat(response.results().get(0).statusLabel()).isEqualTo("Availability pending");
    assertThat(response.results().get(0).availabilitySummary())
        .isEqualTo("Time-slot availability will be published soon.");
    assertThat(response.results().get(0).bookingAvailable()).isFalse();
    assertThat(response.results().get(0).latitude()).isEqualByComparingTo("40.416775");
  }

  @Test
  void normalizesUnsafePaginationAndShortensLongDescriptionsWithoutDroppingAccents() {
    VenueEntity venue = venue("largo", "Local Largo");
    venue.setDescriptionI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(
                SupportedLocale.ES,
                "Descripción pública con acentos repetida para crear un texto suficientemente "
                    + "largo en español que deba recortarse en una tarjeta de resultados sin "
                    + "romper palabras ni perder los caracteres visibles del idioma.")));
    when(venueDao.findPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            anyInt(),
            nullable(String.class),
            nullable(Double.class),
            nullable(Double.class),
            nullable(Double.class),
            anyBoolean(),
            anyString(),
            eq(50),
            eq(0L)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            anyInt(),
            nullable(String.class),
            nullable(Double.class),
            nullable(Double.class),
            nullable(Double.class)))
        .thenReturn(1L);

    var response =
        service.search(
            SupportedLocale.ES, "   ", List.of(), "   ", null, null, null, "unknown", -4, 500);

    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(50);
    assertThat(response.results().get(0).descriptionExcerpt())
        .startsWith("Descripción pública")
        .endsWith("...");
    assertThat(response.results().get(0).descriptionExcerpt()).contains("ñ");
  }

  @Test
  void searchesByNormalizedNameAndKeywordTextWhenQueryIsPresent() {
    VenueEntity venue = venue("cafe-central", "Café Central");
    when(venueDao.findPublishedAdvancedSearch(
            eq("%cafe%"),
            anyList(),
            eq(0),
            nullable(String.class),
            eq(0.0),
            eq(0.0),
            nullable(Double.class),
            eq(false),
            eq("relevance"),
            eq(20),
            eq(0L)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedAdvancedSearch(
            eq("%cafe%"),
            anyList(),
            eq(0),
            nullable(String.class),
            eq(0.0),
            eq(0.0),
            nullable(Double.class)))
        .thenReturn(1L);

    var response =
        service.search(SupportedLocale.ES, "  Café  ", null, null, null, null, null, null, 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).name()).isEqualTo("Café Central");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void filtersByNormalizedCategorySlugsWhenNoQueryIsPresent() {
    VenueEntity venue = venue("casa-luz", "Casa Luz");
    when(venueDao.findPublishedAdvancedSearch(
            nullable(String.class),
            argThat(slugs -> slugs.equals(List.of("restaurante", "pista-de-padel"))),
            eq(2),
            nullable(String.class),
            eq(0.0),
            eq(0.0),
            nullable(Double.class),
            eq(false),
            eq("newest"),
            eq(20),
            eq(0L)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedAdvancedSearch(
            nullable(String.class),
            argThat(slugs -> slugs.equals(List.of("restaurante", "pista-de-padel"))),
            eq(2),
            nullable(String.class),
            eq(0.0),
            eq(0.0),
            nullable(Double.class)))
        .thenReturn(1L);

    var response =
        service.search(
            SupportedLocale.ES,
            null,
            List.of(" Restaurante ", "", "pista-de-padel", "restaurante"),
            null,
            null,
            null,
            null,
            null,
            0,
            20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).categorySlug()).isEqualTo("restaurante");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void filtersByNormalizedLocationWhenNoOtherFilterIsPresent() {
    VenueEntity venue = venue("casa-luz", "Casa Luz");
    when(venueDao.findPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            eq(0),
            eq("%madrid%"),
            eq(0.0),
            eq(0.0),
            nullable(Double.class),
            eq(false),
            eq("newest"),
            eq(20),
            eq(0L)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            eq(0),
            eq("%madrid%"),
            eq(0.0),
            eq(0.0),
            nullable(Double.class)))
        .thenReturn(1L);

    var response =
        service.search(SupportedLocale.ES, null, null, "  MáDRID  ", null, null, null, null, 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).city()).isEqualTo("Madrid");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void combinesTextCategoryLocationRadiusAndDistanceSort() {
    VenueEntity venue = venue("cafe-central", "Café Central");
    when(venueDao.findPublishedAdvancedSearch(
            eq("%cafe%"),
            argThat(slugs -> slugs.equals(List.of("restaurante"))),
            eq(1),
            eq("%madrid%"),
            eq(40.416775),
            eq(-3.703790),
            eq(5000.0),
            eq(true),
            eq("distance"),
            eq(20),
            eq(0L)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedAdvancedSearch(
            eq("%cafe%"),
            argThat(slugs -> slugs.equals(List.of("restaurante"))),
            eq(1),
            eq("%madrid%"),
            eq(40.416775),
            eq(-3.703790),
            eq(5000.0)))
        .thenReturn(1L);

    var response =
        service.search(
            SupportedLocale.ES,
            "  Café  ",
            List.of("restaurante"),
            "Madrid",
            40.416775,
            -3.703790,
            5.0,
            "distance",
            0,
            20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).name()).isEqualTo("Café Central");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void acceptsAvailabilityAndRatingSortModesWithStablePublicFallback() {
    VenueEntity venue = venue("casa-luz", "Casa Luz");
    venue.setManualAvailabilityStatus("available");
    when(venueDao.findPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            eq(0),
            nullable(String.class),
            eq(0.0),
            eq(0.0),
            nullable(Double.class),
            eq(false),
            eq("availability"),
            eq(20),
            eq(0L)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            eq(0),
            nullable(String.class),
            eq(0.0),
            eq(0.0),
            nullable(Double.class)))
        .thenReturn(1L);

    var response =
        service.search(
            SupportedLocale.ES, null, null, null, null, null, null, "availability", 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).slug()).isEqualTo("casa-luz");
    assertThat(response.results().get(0).statusCode()).isEqualTo("available");
    assertThat(response.results().get(0).statusLabel()).isEqualTo("Disponible");
    assertThat(response.results().get(0).availabilitySummary())
        .isEqualTo("Acepta reservas cuando tenga franjas publicadas.");
    assertThat(response.results().get(0).bookingAvailable()).isTrue();
  }

  @Test
  void exposesUnavailableStatusSummaryWhenVenuePausesBookings() {
    VenueEntity venue = venue("pausado", "Local Pausado");
    venue.setManualAvailabilityStatus("unavailable");
    stubAdvancedSearch(List.of(venue), 1L);

    var response =
        service.search(SupportedLocale.ES, null, null, null, null, null, null, null, 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).statusCode()).isEqualTo("unavailable");
    assertThat(response.results().get(0).statusLabel()).isEqualTo("No disponible");
    assertThat(response.results().get(0).availabilitySummary())
        .isEqualTo("El local ha pausado temporalmente las reservas.");
    assertThat(response.results().get(0).bookingAvailable()).isFalse();
  }

  private void stubAdvancedSearch(List<VenueEntity> venues, long totalElements) {
    when(venueDao.findPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            anyInt(),
            nullable(String.class),
            nullable(Double.class),
            nullable(Double.class),
            nullable(Double.class),
            anyBoolean(),
            anyString(),
            anyInt(),
            anyLong()))
        .thenReturn(venues);
    when(venueDao.countPublishedAdvancedSearch(
            nullable(String.class),
            anyList(),
            anyInt(),
            nullable(String.class),
            nullable(Double.class),
            nullable(Double.class),
            nullable(Double.class)))
        .thenReturn(totalElements);
  }

  private static VenueEntity venue(String slug, String name) {
    CategoryEntity category = new CategoryEntity();
    category.setName("Restaurante");
    category.setSlug("restaurante");
    category.setNameI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, "Restaurante", SupportedLocale.EN, "Restaurant")));
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setSlug(slug);
    venue.setName(name);
    venue.setCategory(category);
    venue.setDescription("Cocina de temporada");
    venue.setDescriptionI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(
                SupportedLocale.ES, "Cocina de temporada",
                SupportedLocale.EN, "Seasonal cuisine")));
    venue.setMainImageUrl("/api/public/venue-images/id/main");
    venue.setAddress("Calle Mayor, 1");
    venue.setCity("Madrid");
    venue.setProvince("Madrid");
    venue.setCountry("ES");
    venue.setLatitude(new BigDecimal("40.416775"));
    venue.setLongitude(new BigDecimal("-3.703790"));
    venue.setPublishedAt(Instant.now());
    return venue;
  }
}
