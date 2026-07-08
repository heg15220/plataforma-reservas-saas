package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
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
    when(venueDao.findPublishedForSearch(
            argThat(
                page ->
                    page.getPageNumber() == 0
                        && page.getPageSize() == 20
                        && page.getSort().getOrderFor("publishedAt").isDescending())))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedForSearch()).thenReturn(1L);

    var response = service.search(SupportedLocale.EN, null, null, null, 0, 20);

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
    when(venueDao.findPublishedForSearch(
            argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 50)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedForSearch()).thenReturn(1L);

    var response = service.search(SupportedLocale.ES, "   ", List.of(), "   ", -4, 500);

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
    when(venueDao.findPublishedMatchingSearch(
            argThat(pattern -> pattern.equals("%cafe%")),
            argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedMatchingSearch("%cafe%")).thenReturn(1L);

    var response = service.search(SupportedLocale.ES, "  Café  ", null, null, 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).name()).isEqualTo("Café Central");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void filtersByNormalizedCategorySlugsWhenNoQueryIsPresent() {
    VenueEntity venue = venue("casa-luz", "Casa Luz");
    when(venueDao.findPublishedForSearchByCategories(
            argThat(slugs -> slugs.equals(List.of("restaurante", "pista-de-padel"))),
            argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedForSearchByCategories(
            argThat(slugs -> slugs.equals(List.of("restaurante", "pista-de-padel")))))
        .thenReturn(1L);

    var response =
        service.search(
            SupportedLocale.ES,
            null,
            List.of(" Restaurante ", "", "pista-de-padel", "restaurante"),
            null,
            0,
            20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).categorySlug()).isEqualTo("restaurante");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void combinesTextQueryWithCategoryFilters() {
    VenueEntity venue = venue("cafe-central", "Café Central");
    when(venueDao.findPublishedMatchingSearchByCategories(
            argThat(pattern -> pattern.equals("%cafe%")),
            argThat(slugs -> slugs.equals(List.of("restaurante"))),
            argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedMatchingSearchByCategories("%cafe%", List.of("restaurante")))
        .thenReturn(1L);

    var response =
        service.search(SupportedLocale.ES, "  Café  ", List.of("restaurante"), null, 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).name()).isEqualTo("Café Central");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void filtersByNormalizedLocationWhenNoOtherFilterIsPresent() {
    VenueEntity venue = venue("casa-luz", "Casa Luz");
    when(venueDao.findPublishedForSearchByLocation(
            argThat(pattern -> pattern.equals("%madrid%")),
            argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedForSearchByLocation("%madrid%")).thenReturn(1L);

    var response = service.search(SupportedLocale.ES, null, null, "  MáDRID  ", 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).city()).isEqualTo("Madrid");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void combinesTextCategoryAndLocationFilters() {
    VenueEntity venue = venue("cafe-central", "Café Central");
    when(venueDao.findPublishedMatchingSearchByCategoriesAndLocation(
            argThat(pattern -> pattern.equals("%cafe%")),
            argThat(slugs -> slugs.equals(List.of("restaurante"))),
            argThat(pattern -> pattern.equals("%madrid%")),
            argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20)))
        .thenReturn(List.of(venue));
    when(venueDao.countPublishedMatchingSearchByCategoriesAndLocation(
            "%cafe%", List.of("restaurante"), "%madrid%"))
        .thenReturn(1L);

    var response =
        service.search(SupportedLocale.ES, "  Café  ", List.of("restaurante"), "Madrid", 0, 20);

    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).name()).isEqualTo("Café Central");
    assertThat(response.totalElements()).isEqualTo(1);
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
