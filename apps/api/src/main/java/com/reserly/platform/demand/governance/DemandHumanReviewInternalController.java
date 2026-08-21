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

/** Entrada de servicio para abrir revisión; no ofrece transición ni ejecución. */
@RestController
@RequestMapping(
    path = "/api/internal/demand/v1/governance/reviews",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class DemandHumanReviewInternalController {
  private final DemandHumanReviewService service;

  public DemandHumanReviewInternalController(DemandHumanReviewService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public DemandHumanReviewResponse submit(
      Principal principal, @Valid @RequestBody DemandHumanReviewSubmissionRequest request) {
    return service.submit(principal.getName(), request);
  }
}
