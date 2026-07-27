package com.reserly.platform.incidents.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Demuestra que la respuesta profesional omite identidad, referencias y notas internas. */
class IncidentHistoryConverterTests {

  @Test
  void exposesOnlyTypeDateAndStatus() {
    NoShowIncidentEntity incident = new NoShowIncidentEntity();
    incident.setId(UUID.randomUUID());
    incident.setVenueId(UUID.randomUUID());
    incident.setReservationId(UUID.randomUUID());
    incident.setReportedByUserId(UUID.randomUUID());
    incident.setCustomerEmailNormalized("private@example.com");
    incident.setNotes("Texto interno");
    incident.setIncidentType("no_show");
    incident.setReportedAt(Instant.parse("2026-07-20T12:00:00Z"));
    incident.setStatus("reported");
    var page = new PageImpl<>(List.of(incident), PageRequest.of(0, 25), 1);

    var response = new IncidentHistoryConverter().toResponse(page);

    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.incidentType()).isEqualTo("no_show");
              assertThat(item.reportedAt()).isEqualTo(incident.getReportedAt());
              assertThat(item.status()).isEqualTo("reported");
            });
    assertThat(response.items().getFirst().getClass().getRecordComponents())
        .extracting(java.lang.reflect.RecordComponent::getName)
        .containsExactly("incidentType", "reportedAt", "status");
  }
}
