package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueSearchResponse;
import com.reserly.platform.venues.service.VenuePublicSearchService;
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

  private VenuePublicSearchControllerImpl controller;

  @BeforeEach
  void setUp() {
    controller = new VenuePublicSearchControllerImpl(service);
  }

  @Test
  void explicitLocaleTakesPrecedenceAndDelegatesPagination() {
    VenueSearchResponse response = response("es");
    when(service.search(SupportedLocale.ES, "cafe", List.of("restaurante"), 2, 12))
        .thenReturn(response);

    assertThat(controller.search("es", "cafe", List.of("restaurante"), 2, 12, "en-US").getBody())
        .isSameAs(response);

    verify(service).search(SupportedLocale.ES, "cafe", List.of("restaurante"), 2, 12);
  }

  @Test
  void negotiatesSpanishAndFallsBackToEnglish() {
    when(service.search(SupportedLocale.ES, null, null, 0, 20)).thenReturn(response("es"));
    when(service.search(SupportedLocale.EN, null, null, 0, 20)).thenReturn(response("en"));

    controller.search(null, null, null, 0, 20, "es-ES,es;q=0.9");
    controller.search("fr", null, null, 0, 20, "es");

    verify(service).search(SupportedLocale.ES, null, null, 0, 20);
    verify(service).search(SupportedLocale.EN, null, null, 0, 20);
  }

  private static VenueSearchResponse response(String locale) {
    return new VenueSearchResponse(locale, 0, 20, 0, 0, false, List.of());
  }
}
