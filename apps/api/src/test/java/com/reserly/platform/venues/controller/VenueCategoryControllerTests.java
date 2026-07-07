package com.reserly.platform.venues.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueCategoryResponse;
import com.reserly.platform.venues.service.VenueCategoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Prueba la negociación de idioma del endpoint de categorías activas. */
@ExtendWith(MockitoExtension.class)
class VenueCategoryControllerTests {

  @Mock private VenueCategoryService service;

  private VenueCategoryControllerImpl controller;

  @BeforeEach
  void setUp() {
    controller = new VenueCategoryControllerImpl(service);
  }

  @Test
  void usesExplicitLocaleBeforeAcceptLanguage() {
    var categories =
        List.of(new VenueCategoryResponse(UUID.randomUUID(), "restaurante", "Restaurant"));
    when(service.findActive(SupportedLocale.EN)).thenReturn(categories);

    var response = controller.findActive("en", "es-ES,es;q=0.9");

    assertThat(response.getBody()).isEqualTo(categories);
    verify(service).findActive(SupportedLocale.EN);
  }

  @Test
  void fallsBackToSpanishFromAcceptLanguageOrEnglishByDefault() {
    controller.findActive(null, "es-ES,es;q=0.9");
    controller.findActive("fr", "es-ES,es;q=0.9");

    verify(service).findActive(SupportedLocale.ES);
    verify(service).findActive(SupportedLocale.EN);
  }
}
