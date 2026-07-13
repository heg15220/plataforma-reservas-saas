package com.reserly.platform.forms.dto;

import java.util.List;

/** Esquema completo, ordenado e inmutable que el panel puede renderizar como formulario real. */
public record ReservationFormPreviewResponse(List<ReservationFormPreviewFieldResponse> fields) {

  public ReservationFormPreviewResponse {
    fields = List.copyOf(fields);
  }
}
