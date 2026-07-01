package com.reserly.platform.venues.service;

import com.reserly.platform.localization.LocalizedText;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Política Unicode que limita cada versión localizada de la descripción a 350 palabras.
 *
 * <p>Una palabra contiene letras o números Unicode y puede incluir apóstrofes o guiones internos.
 * La puntuación restante, los símbolos y los emojis actúan como separadores y no cuentan.
 */
@Service
public class VenueDescriptionServiceImpl implements VenueDescriptionService {

  public static final int MAX_WORDS = 350;

  private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*");

  @Override
  public void validate(LocalizedText description) {
    if (description == null) {
      return;
    }

    description
        .values()
        .forEach(
            (locale, text) -> {
              int actualWords = countWords(text);
              if (actualWords > MAX_WORDS) {
                throw new VenueDescriptionTooLongException(locale, actualWords, MAX_WORDS);
              }
            });
  }

  private int countWords(String text) {
    Matcher matcher = WORD.matcher(text);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }
}
