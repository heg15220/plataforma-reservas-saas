package com.reserly.platform.demand.ingestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Verifica el contrato HTTP opaco sin levantar módulos Spring no relacionados. */
class DemandEventIngestionControllerTests {

  private static final Principal PRINCIPAL = () -> "test-producer";

  @Test
  void returnsIdempotentBatchSummary() throws Exception {
    DemandEventIngestionService service = mock(DemandEventIngestionService.class);
    UUID eventId = UUID.randomUUID();
    when(service.ingest(eq("test-producer"), any()))
        .thenReturn(
            new EventBatchIngestionResponse(
                1, 0, List.of(new EventIngestionItemResponse(eventId, "accepted"))));
    mockMvc(service)
        .perform(
            post("/api/internal/demand/v1/events")
                .principal(PRINCIPAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(eventId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acceptedCount").value(1))
        .andExpect(jsonPath("$.results[0].status").value("accepted"));
  }

  @Test
  void neverReflectsRejectedPayloadOrInternalCode() throws Exception {
    DemandEventIngestionService service = mock(DemandEventIngestionService.class);
    when(service.ingest(eq("test-producer"), any()))
        .thenThrow(new DemandIngestionException("CONTEXT_INVALID"));
    mockMvc(service)
        .perform(
            post("/api/internal/demand/v1/events")
                .principal(PRINCIPAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"error\":\"EVENT_INVALID\"}"));
  }

  private MockMvc mockMvc(DemandEventIngestionService service) {
    return MockMvcBuilders.standaloneSetup(new DemandEventIngestionControllerImpl(service))
        .setControllerAdvice(new DemandIngestionExceptionHandler())
        .build();
  }

  private String validBody(UUID eventId) {
    return """
        {
          "events": [{
            "eventId": "%s",
            "schemaVersion": 1,
            "eventType": "searchPerformed",
            "occurredAt": "2026-08-13T11:59:59Z",
            "requestId": "%s",
            "purpose": "analytics",
            "sessionId": "%s",
            "countryCode": "ES",
            "context": {"queryLength": 12}
          }]
        }
        """
        .formatted(eventId, UUID.randomUUID(), UUID.randomUUID());
  }
}
