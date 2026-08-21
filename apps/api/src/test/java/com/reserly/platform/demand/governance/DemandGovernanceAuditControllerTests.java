package com.reserly.platform.demand.governance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Verifica principal autenticado, respuesta opaca y Bean Validation del endpoint interno. */
class DemandGovernanceAuditControllerTests {

  @Test
  void recordsWithAuthenticatedServiceAndReturnsOpaqueIdentity() throws Exception {
    DemandGovernanceAuditService service = mock(DemandGovernanceAuditService.class);
    UUID eventId = UUID.randomUUID();
    UUID auditId = UUID.randomUUID();
    when(service.recordSystem(eq("prefect-worker-v1"), any()))
        .thenReturn(
            new DemandGovernanceAuditResponse(
                auditId, eventId, "model", "registered", Instant.parse("2026-08-21T12:00:00Z")));

    mockMvc(service)
        .perform(
            post("/api/internal/demand/v1/governance/audit")
                .principal((Principal) () -> "prefect-worker-v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(eventId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.auditLogId").value(auditId.toString()))
        .andExpect(jsonPath("$.eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.resourceType").value("model"))
        .andExpect(jsonPath("$.action").value("registered"));
  }

  @Test
  void rejectsInvalidReasonBeforeCallingService() throws Exception {
    DemandGovernanceAuditService service = mock(DemandGovernanceAuditService.class);
    String invalid = body(UUID.randomUUID()).replace("scheduled_review", "Manual reason");

    mockMvc(service)
        .perform(
            post("/api/internal/demand/v1/governance/audit")
                .principal((Principal) () -> "prefect-worker-v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
        .andExpect(status().isBadRequest());
  }

  @Test
  void divergentReplayReturnsOpaqueConflict() throws Exception {
    DemandGovernanceAuditService service = mock(DemandGovernanceAuditService.class);
    when(service.recordSystem(eq("prefect-worker-v1"), any()))
        .thenThrow(new IllegalArgumentException("DEMAND_GOVERNANCE_EVENT_ID_CONFLICT"));

    mockMvc(service)
        .perform(
            post("/api/internal/demand/v1/governance/audit")
                .principal((Principal) () -> "prefect-worker-v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("GOVERNANCE_AUDIT_CONFLICT"));
  }

  private MockMvc mockMvc(DemandGovernanceAuditService service) {
    return MockMvcBuilders.standaloneSetup(new DemandGovernanceAuditController(service))
        .setControllerAdvice(new DemandGovernanceAuditExceptionHandler())
        .build();
  }

  private String body(UUID eventId) {
    return """
        {
          "eventId": "%s",
          "resourceType": "model",
          "resourceKey": "ranking:model-v2",
          "action": "registered",
          "reasonCode": "scheduled_review",
          "beforeVersion": "model-v1",
          "afterVersion": "model-v2",
          "policyVersion": "governance-policy-v1",
          "artifactSha256": "%s",
          "effectiveFrom": "2026-08-21T12:00:00Z",
          "effectiveUntil": "2026-09-21T12:00:00Z",
          "correlationId": "%s",
          "automated": false,
          "approvalReference": "admin-approval-v1"
        }
        """
        .formatted(eventId, "a".repeat(64), UUID.randomUUID());
  }
}
