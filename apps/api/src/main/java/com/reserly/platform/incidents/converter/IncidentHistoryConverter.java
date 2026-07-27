package com.reserly.platform.incidents.converter;

import com.reserly.platform.incidents.dto.IncidentHistoryItemResponse;
import com.reserly.platform.incidents.dto.IncidentHistoryResponse;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/** Convierte entidades privadas en una página sin identificadores ni texto libre. */
@Component
public class IncidentHistoryConverter {

  public IncidentHistoryResponse toResponse(Page<NoShowIncidentEntity> incidents) {
    return new IncidentHistoryResponse(
        incidents.getNumber(),
        incidents.getSize(),
        incidents.getTotalElements(),
        incidents.getTotalPages(),
        incidents.getContent().stream()
            .map(
                incident ->
                    new IncidentHistoryItemResponse(
                        incident.getIncidentType(), incident.getReportedAt(), incident.getStatus()))
            .toList());
  }
}
