package com.reserly.platform.forms.dto;

import java.util.List;
import java.util.UUID;

/** Campo renderizable con textos localizados para la previsualizaci?n privada. */
public record ReservationFormPreviewFieldResponse(
    UUID id,
    String source,
    String key,
    String type,
    String label,
    String labelKey,
    ReservationFormLocalizedTextDto labelI18n,
    boolean required,
    boolean editable,
    List<String> options,
    List<ReservationFormLocalizedTextDto> optionsI18n,
    int position) {

  public ReservationFormPreviewFieldResponse {
    options = options == null ? null : List.copyOf(options);
    optionsI18n = optionsI18n == null ? null : List.copyOf(optionsI18n);
  }
}
