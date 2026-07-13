package com.reserly.platform.forms.dto;

import java.time.Instant;
import java.util.List;

/** Estado editorial privado sin exponer los textos ni datos del propietario. */
public record ReservationFormPublicationResponse(
    boolean published,
    boolean fallbackApproved,
    boolean fullyTranslated,
    List<String> missingTranslations,
    Instant publishedAt) {

  public ReservationFormPublicationResponse {
    missingTranslations = List.copyOf(missingTranslations);
  }
}
