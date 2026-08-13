package com.reserly.platform.demand.attribute.admin;

import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API exclusivamente administrativa para revisar y versionar la ontología de demanda. */
@RestController
@RequestMapping(path = "/api/admin/demand-ontology", produces = MediaType.APPLICATION_JSON_VALUE)
public class DemandAttributeAdminController {
  private final DemandAttributeGovernanceService service;

  public DemandAttributeAdminController(DemandAttributeGovernanceService service) {
    this.service = service;
  }

  /** Lista catálogo y cola en una lectura consistente. */
  @GetMapping
  public DemandAttributeAdminListResponse list() {
    return service.list();
  }

  /** Registra una propuesta en borrador; no la hace utilizable por el motor. */
  @PostMapping(path = "/candidates", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DemandAttributeCandidateAdminResponse> createCandidate(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody DemandAttributeCandidateRequest request,
      HttpServletRequest servletRequest) {
    DemandAttributeCandidateAdminResponse result =
        service.createCandidate(account.userId(), request, context(servletRequest));
    return ResponseEntity.created(
            URI.create("/api/admin/demand-ontology/candidates/" + result.id()))
        .body(result);
  }

  /** Aplica una transición validada y auditada a una propuesta. */
  @PostMapping(
      path = "/candidates/{candidateId}/transition",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public DemandAttributeCandidateAdminResponse transitionCandidate(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID candidateId,
      @Valid @RequestBody DemandAttributeTransitionRequest request,
      HttpServletRequest servletRequest) {
    return service.transitionCandidate(
        account.userId(), candidateId, request, context(servletRequest));
  }

  /** Fusiona o retira un término sin borrar sus referencias históricas. */
  @PostMapping(
      path = "/attributes/{attributeId}/transition",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public DemandAttributeAdminResponse transitionAttribute(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID attributeId,
      @Valid @RequestBody DemandAttributeTransitionRequest request,
      HttpServletRequest servletRequest) {
    return service.transitionAttribute(
        account.userId(), attributeId, request, context(servletRequest));
  }

  private AdminRequestContext context(HttpServletRequest request) {
    return new AdminRequestContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
  }
}
