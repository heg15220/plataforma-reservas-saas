package com.reserly.platform.localization;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Locales soportados por Reserly en contratos backend, datos persistidos y respuestas REST.
 *
 * <p>La persistencia usa etiquetas BCP 47 en minúsculas (`es`, `en`) para que los documentos JSONB
 * sean estables aunque los nombres Java cambien. Las variantes regionales solo se usan como entrada
 * de resolución; los datos localizados se almacenan en los locales base soportados.
 */
public enum SupportedLocale {
  ES("es"),
  EN("en");

  private final String languageTag;

  SupportedLocale(String languageTag) {
    this.languageTag = languageTag;
  }

  /**
   * Devuelve la etiqueta persistida y expuesta en contratos JSON.
   *
   * @return etiqueta BCP 47 soportada
   */
  public String languageTag() {
    return languageTag;
  }

  /**
   * Resuelve una etiqueta exacta persistida, sin aplicar reglas de navegador ni fallback.
   *
   * @param value etiqueta recibida desde JSON, configuración o payload REST
   * @return locale soportado si la etiqueta existe
   */
  public static Optional<SupportedLocale> fromLanguageTag(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(locale -> locale.languageTag.equals(normalizedValue))
        .findFirst();
  }
}
