package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.reviews.dto.PublicReviewCollectionResponse;
import com.reserly.platform.venues.dto.VenuePublicProfileResponse;
import com.reserly.platform.venues.service.VenuePublicProfileService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica la negociación de idioma y el contrato REST anónimo de la ficha. */
@ExtendWith(MockitoExtension.class)
class VenuePublicProfileControllerTests {

  @Mock private VenuePublicProfileService service;

  private VenuePublicProfileControllerImpl controller;

  @BeforeEach
  void setUp() {
    controller = new VenuePublicProfileControllerImpl(service);
  }

  @Test
  void explicitSupportedLocaleTakesPrecedence() {
    VenuePublicProfileResponse response = response("es");
    when(service.findBySlug("casa-luz", SupportedLocale.ES)).thenReturn(response);

    assertThat(controller.find("casa-luz", "es", "en-US").getBody()).isSameAs(response);
  }

  @Test
  void negotiatesSpanishAndFallsBackToEnglish() {
    when(service.findBySlug("casa-luz", SupportedLocale.ES)).thenReturn(response("es"));
    when(service.findBySlug("otro", SupportedLocale.EN)).thenReturn(response("en"));

    controller.find("casa-luz", null, "es-ES,es;q=0.9");
    controller.find("otro", "fr", "es");

    verify(service).findBySlug("casa-luz", SupportedLocale.ES);
    verify(service).findBySlug("otro", SupportedLocale.EN);
  }

  private static VenuePublicProfileResponse response(String locale) {
    return new VenuePublicProfileResponse(
        "casa-luz",
        locale,
        "Casa Luz",
        "restaurante",
        "Restaurante",
        "Descripción",
        null,
        null,
        null,
        "/main",
        List.of(),
        List.of(),
        "Calle Mayor, 1",
        "Madrid",
        null,
        "ES",
        null,
        new BigDecimal("40.416775"),
        new BigDecimal("-3.703790"),
        null,
        null,
        new PublicReviewCollectionResponse(null, 0, false, List.of()));
  }
}
