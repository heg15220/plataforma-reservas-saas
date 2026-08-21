package com.reserly.platform.demand.governance;

import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cola administrativa de revisión y corrección de impacto material. */
@RestController
@RequestMapping(
    path = "/api/admin/demand-governance/reviews",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class DemandHumanReviewAdminController {
  private final DemandHumanReviewService service;

  public DemandHumanReviewAdminController(DemandHumanReviewService service) {
    this.service = service;
  }

  @GetMapping
  public List<DemandHumanReviewResponse> list() {
    return service.listAdmin();
  }

  @PostMapping(path = "/{reviewId}/decision", consumes = MediaType.APPLICATION_JSON_VALUE)
  public DemandHumanReviewResponse decide(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID reviewId,
      @Valid @RequestBody DemandHumanReviewDecisionRequest request,
      HttpServletRequest servletRequest) {
    return service.decide(account.userId(), reviewId, request, context(servletRequest));
  }

  private AdminRequestContext context(HttpServletRequest request) {
    return new AdminRequestContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
  }
}
