package com.reserly.platform.venues.controller;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueCategoryResponse;
import com.reserly.platform.venues.service.VenueCategoryService;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que reduce variantes de idioma a los locales base soportados. */
@RestController
public class VenueCategoryControllerImpl implements VenueCategoryController {

  private final VenueCategoryService service;

  public VenueCategoryControllerImpl(VenueCategoryService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<List<VenueCategoryResponse>> findActive(
      String locale, String acceptLanguage) {
    return ResponseEntity.ok(service.findActive(resolveLocale(locale, acceptLanguage)));
  }

  private static SupportedLocale resolveLocale(String requested, String acceptLanguage) {
    if (requested != null && !requested.isBlank()) {
      return SupportedLocale.fromLanguageTag(requested).orElse(SupportedLocale.EN);
    }
    if (acceptLanguage != null && acceptLanguage.trim().toLowerCase(Locale.ROOT).startsWith("es")) {
      return SupportedLocale.ES;
    }
    return SupportedLocale.EN;
  }
}
