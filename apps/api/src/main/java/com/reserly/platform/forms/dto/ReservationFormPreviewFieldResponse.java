package com.reserly.platform.forms.dto;

import java.util.List;
import java.util.UUID;

/**
 * Campo renderizable de la previsualización privada.
 *
 * <p>Los campos base usan labelKey y no tienen id; los personalizados usan label e id persistido.
 */
public record ReservationFormPreviewFieldResponse(
    UUID id,
    String source,
    String key,
    String type,
    String label,
    String labelKey,
    boolean required,
    boolean editable,
    List<String> options,
    int position) {

  public ReservationFormPreviewFieldResponse {
    options = options == null ? null : List.copyOf(options);
  }
}
