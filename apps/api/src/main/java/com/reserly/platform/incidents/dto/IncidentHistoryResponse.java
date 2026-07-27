package com.reserly.platform.incidents.dto;

import java.util.List;

/** Página de incidencias operativas recientes asociadas a la identidad acreditada. */
public record IncidentHistoryResponse(
    int page,
    int size,
    long totalElements,
    int totalPages,
    List<IncidentHistoryItemResponse> items) {

  public IncidentHistoryResponse {
    items = List.copyOf(items);
  }
}
