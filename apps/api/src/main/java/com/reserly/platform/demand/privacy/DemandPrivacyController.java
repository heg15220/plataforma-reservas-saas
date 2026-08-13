package com.reserly.platform.demand.privacy;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontera interna autenticada para derechos ya verificados por el sistema responsable. */
@RestController
@RequestMapping(
    path = "/api/internal/demand/v1/privacy",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class DemandPrivacyController {
  private final DemandPrivacyService service;

  public DemandPrivacyController(DemandPrivacyService service) {
    this.service = service;
  }

  @PostMapping(path = "/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
  public DemandPrivacyResponse execute(@Valid @RequestBody DemandPrivacyRequest request) {
    return service.execute(request);
  }
}
