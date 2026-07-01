package com.reserly.platform.venues.service;

import com.reserly.platform.localization.SupportedLocale;

/**
 * Señala que una traducción de la descripción excede el límite publicable.
 *
 * <p>Expone únicamente metadatos seguros para que el cliente identifique el idioma y solicite
 * acortar el texto sin devolver su contenido.
 */
public class VenueDescriptionTooLongException extends RuntimeException {

  private final SupportedLocale locale;
  private final int actualWords;
  private final int maxWords;

  public VenueDescriptionTooLongException(SupportedLocale locale, int actualWords, int maxWords) {
    super("La descripción supera el máximo de palabras permitido.");
    this.locale = locale;
    this.actualWords = actualWords;
    this.maxWords = maxWords;
  }

  public SupportedLocale getLocale() {
    return locale;
  }

  public int getActualWords() {
    return actualWords;
  }

  public int getMaxWords() {
    return maxWords;
  }
}
