package com.reserly.platform.demand.governance;

import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mecanismo privado para que el local afectado impugne una decisión material una sola vez. */
@RestController
@RequestMapping(
    path = "/api/venue/me/demand-governance/reviews",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class DemandHumanReviewVenueController {
  private final DemandHumanReviewService service;

  public DemandHumanReviewVenueController(DemandHumanReviewService service) {
    this.service = service;
  }

  @PostMapping(path = "/{reviewId}/appeal", consumes = MediaType.APPLICATION_JSON_VALUE)
  public DemandHumanReviewResponse appeal(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID reviewId,
      @Valid @RequestBody DemandHumanReviewAppealRequest request,
      HttpServletRequest servletRequest) {
    return service.appeal(
        account.userId(),
        reviewId,
        request,
        new AdminRequestContext(
            servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
  }
}
