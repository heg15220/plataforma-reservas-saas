package com.reserly.platform.localization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pruebas del contrato backend para textos localizados persistidos en JSONB. */
class LocalizedTextTests {

  @Test
  void resolvesRequestedLocaleBeforeFallback() {
    LocalizedText text =
        new LocalizedText(
            SupportedLocale.ES, Map.of(SupportedLocale.ES, "Carta", SupportedLocale.EN, "Menu"));

    assertThat(text.resolve(SupportedLocale.ES)).contains("Carta");
    assertThat(text.resolve(SupportedLocale.EN)).contains("Menu");
  }

  @Test
  void fallsBackToEnglishAndThenSourceLocale() {
    LocalizedText englishFallback =
        new LocalizedText(
            SupportedLocale.ES, Map.of(SupportedLocale.ES, "Carta", SupportedLocale.EN, "Menu"));
    LocalizedText sourceFallback =
        new LocalizedText(SupportedLocale.ES, Map.of(SupportedLocale.ES, "Carta"));

    assertThat(englishFallback.resolve(null)).contains("Menu");
    assertThat(sourceFallback.resolve(SupportedLocale.EN)).contains("Carta");
  }

  @Test
  void detectsMissingRequiredTranslationsBeforePublishing() {
    LocalizedText text = new LocalizedText(SupportedLocale.ES, Map.of(SupportedLocale.ES, "Carta"));
    Set<SupportedLocale> requiredLocales = Set.of(SupportedLocale.ES, SupportedLocale.EN);

    assertThat(text.hasRequiredTranslations(requiredLocales)).isFalse();
    assertThat(text.missingTranslations(requiredLocales)).containsExactly(SupportedLocale.EN);
  }

  @Test
  void mapsPersistedLanguageTagsToSupportedLocales() {
    LocalizedText text =
        LocalizedText.fromLanguageTagValues(
            "es", Map.of("es", "Carta de temporada", "en", "Seasonal menu", "fr", "Menu"));

    assertThat(text.sourceLocale()).isEqualTo(SupportedLocale.ES);
    assertThat(text.toLanguageTagValues())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("es", "Carta de temporada", "en", "Seasonal menu"));
  }

  @Test
  void sanitizesEveryLocalizedValueBeforeItCanBePersisted() {
    LocalizedText text =
        LocalizedText.fromLanguageTagValues(
            "es",
            Map.of(
                "es", "<img src=x onerror=alert(1)>Carta segura",
                "en", "<script>alert(2)</script>Safe menu"));

    assertThat(text.toLanguageTagValues())
        .containsEntry("es", "Carta segura")
        .containsEntry("en", "Safe menu");
  }

  @Test
  void requiresSourceLocaleAndVisibleSourceText() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LocalizedText(null, Map.of(SupportedLocale.ES, "Carta")));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new LocalizedText(SupportedLocale.ES, Map.of(SupportedLocale.EN, "Menu")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalizedText.fromLanguageTagValues("fr", Map.of("fr", "Menu")));
  }

  @Test
  void serializesPersistedLocalesAsLowercaseLanguageTags() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    LocalizedText original =
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, "Servicios", SupportedLocale.EN, "Services"));

    String json = objectMapper.writeValueAsString(original);
    LocalizedText restored = objectMapper.readValue(json, LocalizedText.class);

    assertThat(json)
        .contains("\"sourceLocale\":\"es\"", "\"es\":\"Servicios\"", "\"en\":\"Services\"");
    assertThat(restored).isEqualTo(original);
  }
}
