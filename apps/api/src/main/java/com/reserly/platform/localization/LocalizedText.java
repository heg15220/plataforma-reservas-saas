package com.reserly.platform.localization;

import com.reserly.platform.infrastructure.validation.PlainTextSanitizer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Value object para textos configurables almacenados en base de datos.
 *
 * <p>El patrón físico recomendado es una columna JSONB con nombre conceptual `*_i18n` y nombre
 * físico `lowerCamelCase`, por ejemplo `"descriptionI18n"`. El documento debe persistir el idioma
 * origen y un mapa de traducciones por locale soportado:
 *
 * <pre>
 * {
 *   "sourceLocale": "es",
 *   "values": {
 *     "es": "Carta de temporada",
 *     "en": "Seasonal menu"
 *   }
 * }
 * </pre>
 *
 * <p>Este objeto no traduce contenido automáticamente. Solo normaliza el contrato, valida que el
 * idioma origen tenga texto y resuelve el texto visible con fallback controlado.
 *
 * @param sourceLocale idioma en el que se creó o editó originalmente el contenido
 * @param values textos por locale soportado
 */
public record LocalizedText(SupportedLocale sourceLocale, Map<SupportedLocale, String> values) {

  private static final SupportedLocale FALLBACK_LOCALE = SupportedLocale.EN;

  public LocalizedText {
    if (sourceLocale == null) {
      throw new IllegalArgumentException("El idioma origen del texto localizado es obligatorio.");
    }

    values = immutableNormalizedValues(values);

    if (!hasVisibleValue(values, sourceLocale)) {
      throw new IllegalArgumentException(
          "El texto localizado debe incluir un valor no vacío para su idioma origen.");
    }
  }

  /**
   * Construye un texto localizado desde claves de idioma persistidas como `es` y `en`.
   *
   * @param sourceLocaleTag idioma origen persistido
   * @param languageTagValues mapa de textos con claves BCP 47 soportadas
   * @return value object normalizado
   */
  public static LocalizedText fromLanguageTagValues(
      String sourceLocaleTag, Map<String, String> languageTagValues) {
    SupportedLocale sourceLocale =
        SupportedLocale.fromLanguageTag(sourceLocaleTag)
            .orElseThrow(() -> new IllegalArgumentException("Idioma origen no soportado."));

    EnumMap<SupportedLocale, String> localizedValues = new EnumMap<>(SupportedLocale.class);

    if (languageTagValues != null) {
      for (Map.Entry<String, String> entry : languageTagValues.entrySet()) {
        SupportedLocale.fromLanguageTag(entry.getKey())
            .ifPresent(locale -> localizedValues.put(locale, entry.getValue()));
      }
    }

    return new LocalizedText(sourceLocale, localizedValues);
  }

  /**
   * Resuelve el texto visible para un locale, usando fallback `en` y finalmente el idioma origen.
   *
   * @param requestedLocale idioma efectivo de la request
   * @return texto visible si existe algún valor utilizable
   */
  public Optional<String> resolve(SupportedLocale requestedLocale) {
    return firstVisibleValue(requestedLocale)
        .or(() -> firstVisibleValue(FALLBACK_LOCALE))
        .or(() -> firstVisibleValue(sourceLocale));
  }

  /**
   * Indica si el contenido puede publicarse exigiendo valores para todos los locales dados.
   *
   * @param requiredLocales locales obligatorios para el flujo de publicación
   * @return {@code true} cuando todos los locales exigidos tienen texto visible
   */
  public boolean hasRequiredTranslations(Set<SupportedLocale> requiredLocales) {
    return requiredLocales.stream().allMatch(locale -> hasVisibleValue(values, locale));
  }

  /**
   * Devuelve los locales obligatorios que no tienen texto visible.
   *
   * @param requiredLocales locales obligatorios para el flujo de publicación
   * @return conjunto de locales pendientes
   */
  public Set<SupportedLocale> missingTranslations(Set<SupportedLocale> requiredLocales) {
    return requiredLocales.stream()
        .filter(locale -> !hasVisibleValue(values, locale))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Convierte los textos a un mapa apto para serialización JSON estable.
   *
   * @return mapa con claves `es` y/o `en`
   */
  public Map<String, String> toLanguageTagValues() {
    return values.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                entry -> entry.getKey().languageTag(), Map.Entry::getValue));
  }

  private Optional<String> firstVisibleValue(SupportedLocale locale) {
    if (locale == null || !hasVisibleValue(values, locale)) {
      return Optional.empty();
    }

    return Optional.of(values.get(locale));
  }

  private static Map<SupportedLocale, String> immutableNormalizedValues(
      Map<SupportedLocale, String> sourceValues) {
    EnumMap<SupportedLocale, String> normalizedValues = new EnumMap<>(SupportedLocale.class);

    if (sourceValues != null) {
      for (Map.Entry<SupportedLocale, String> entry : sourceValues.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          String sanitized = PlainTextSanitizer.sanitizeNullable(entry.getValue());
          if (sanitized != null) {
            normalizedValues.put(entry.getKey(), sanitized);
          }
        }
      }
    }

    return Map.copyOf(normalizedValues);
  }

  private static boolean hasVisibleValue(
      Map<SupportedLocale, String> values, SupportedLocale locale) {
    String value = values.get(locale);
    return value != null && !value.isBlank();
  }
}
