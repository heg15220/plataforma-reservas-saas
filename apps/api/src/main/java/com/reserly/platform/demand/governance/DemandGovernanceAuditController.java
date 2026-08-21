package com.reserly.platform.demand.governance;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Frontera interna autenticada para registrar evidencia; no modifica el recurso auditado. */
@RestController
@RequestMapping(
    path = "/api/internal/demand/v1/governance/audit",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class DemandGovernanceAuditController {
  private final DemandGovernanceAuditService service;

  public DemandGovernanceAuditController(DemandGovernanceAuditService service) {
    this.service = service;
  }

  /** Registra idempotentemente un evento y devuelve solo su identidad administrativa. */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public DemandGovernanceAuditResponse record(
      Principal principal, @Valid @RequestBody DemandGovernanceAuditRequest request) {
    return service.recordSystem(principal.getName(), request);
  }
}
