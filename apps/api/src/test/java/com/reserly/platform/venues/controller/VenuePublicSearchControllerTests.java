package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchResponse;
import com.reserly.platform.venues.dto.VenueSearchSuggestionsResponse;
import com.reserly.platform.venues.service.VenuePublicSearchService;
import com.reserly.platform.venues.service.VenueSearchSuggestionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el contrato REST anónimo de búsqueda pública y la resolución de idioma. */
@ExtendWith(MockitoExtension.class)
class VenuePublicSearchControllerTests {

  @Mock private VenuePublicSearchService service;
  @Mock private VenueSearchSuggestionService suggestionService;

  private VenuePublicSearchControllerImpl controller;

  @BeforeEach
  void setUp() {
    controller = new VenuePublicSearchControllerImpl(service, suggestionService);
  }

  @Test
  void explicitLocaleTakesPrecedenceAndDelegatesPagination() {
    VenueSearchResponse response = response("es");
    when(service.search(
            SupportedLocale.ES,
            "cafe",
            List.of("restaurante"),
            "Madrid",
            40.416775,
            -3.703790,
            5.0,
            "distance",
            2,
            12))
        .thenReturn(response);

    assertThat(
            controller
                .search(
                    "es",
                    "cafe",
                    List.of("restaurante"),
                    "Madrid",
                    40.416775,
                    -3.703790,
                    5.0,
                    "distance",
                    2,
                    12,
                    "en-US")
                .getBody())
        .isSameAs(response);

    verify(service)
        .search(
            SupportedLocale.ES,
            "cafe",
            List.of("restaurante"),
            "Madrid",
            40.416775,
            -3.703790,
            5.0,
            "distance",
            2,
            12);
  }

  @Test
  void negotiatesSpanishAndFallsBackToEnglish() {
    when(service.search(SupportedLocale.ES, null, null, null, null, null, null, null, 0, 20))
        .thenReturn(response("es"));
    when(service.search(SupportedLocale.EN, null, null, null, null, null, null, null, 0, 20))
        .thenReturn(response("en"));

    controller.search(null, null, null, null, null, null, null, null, 0, 20, "es-ES,es;q=0.9");
    controller.search("fr", null, null, null, null, null, null, null, 0, 20, "es");

    verify(service).search(SupportedLocale.ES, null, null, null, null, null, null, null, 0, 20);
    verify(service).search(SupportedLocale.EN, null, null, null, null, null, null, null, 0, 20);
  }

  @Test
  void delegatesBoundedSuggestionsAndPublishesShortCacheHeader() {
    VenueSearchSuggestionsResponse response = new VenueSearchSuggestionsResponse("es", List.of());
    when(suggestionService.suggest(SupportedLocale.ES, "location", "mad", 8)).thenReturn(response);

    var result = controller.suggestions("es", "location", "mad", 8, "en-US");

    assertThat(result.getBody()).isSameAs(response);
    assertThat(result.getHeaders().getCacheControl())
        .isEqualTo("public, max-age=30, stale-while-revalidate=120");
    verify(suggestionService).suggest(SupportedLocale.ES, "location", "mad", 8);
  }

  private static VenueSearchResponse response(String locale) {
    return new VenueSearchResponse(locale, 0, 20, 0, 0, false, List.of());
  }
}
