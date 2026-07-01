package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifica los límites y la semántica Unicode del contador de palabras de descripciones. */
class VenueDescriptionServiceTests {

  private final VenueDescriptionService service = new VenueDescriptionServiceImpl();

  @Test
  void acceptsAnAbsentDescriptionAndExactlyThreeHundredFiftyWords() {
    assertThatCode(() -> service.validate(null)).doesNotThrowAnyException();
    assertThatCode(() -> service.validate(localized(words(350)))).doesNotThrowAnyException();
  }

  @Test
  void rejectsThreeHundredFiftyOneWordsWithSafeLimitMetadata() {
    assertThatThrownBy(() -> service.validate(localized(words(351))))
        .isInstanceOfSatisfying(
            VenueDescriptionTooLongException.class,
            exception -> {
              org.assertj.core.api.Assertions.assertThat(exception.getLocale())
                  .isEqualTo(SupportedLocale.ES);
              org.assertj.core.api.Assertions.assertThat(exception.getActualWords()).isEqualTo(351);
              org.assertj.core.api.Assertions.assertThat(exception.getMaxWords()).isEqualTo(350);
            });
  }

  @Test
  void validatesEveryPublishedLanguageIndependently() {
    LocalizedText description =
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, words(350), SupportedLocale.EN, words(351)));

    assertThatThrownBy(() -> service.validate(description))
        .isInstanceOfSatisfying(
            VenueDescriptionTooLongException.class,
            exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getLocale())
                    .isEqualTo(SupportedLocale.EN));
  }

  @Test
  void countsUnicodeWordsAndKeepsInternalApostrophesAndHyphensTogether() {
    String fiveWords = "¡Café! fútbol-sala rock'n'roll pádel 2026 😊";
    String exactlyAtLimit = String.join(" ", Collections.nCopies(70, fiveWords));

    assertThatCode(() -> service.validate(localized(exactlyAtLimit))).doesNotThrowAnyException();
    assertThatThrownBy(() -> service.validate(localized(exactlyAtLimit + " extra")))
        .isInstanceOf(VenueDescriptionTooLongException.class);
  }

  private LocalizedText localized(String text) {
    return new LocalizedText(SupportedLocale.ES, Map.of(SupportedLocale.ES, text));
  }

  private String words(int count) {
    return String.join(" ", Collections.nCopies(count, "palabra"));
  }
}
