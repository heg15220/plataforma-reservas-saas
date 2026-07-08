package com.reserly.platform.venues.controller;

import com.reserly.platform.localization.SupportedLocale;
import java.util.Locale;

/** Normaliza el idioma solicitado por endpoints públicos anónimos de locales. */
final class VenuePublicLocaleResolver {

  private VenuePublicLocaleResolver() {}

  static SupportedLocale resolve(String requested, String acceptLanguage) {
    if (requested != null && !requested.isBlank()) {
      return SupportedLocale.fromLanguageTag(requested).orElse(SupportedLocale.EN);
    }
    if (acceptLanguage != null && acceptLanguage.trim().toLowerCase(Locale.ROOT).startsWith("es")) {
      return SupportedLocale.ES;
    }
    return SupportedLocale.EN;
  }
}
