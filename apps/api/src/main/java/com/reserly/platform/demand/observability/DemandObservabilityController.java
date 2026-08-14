package com.reserly.platform.demand.observability;

import java.time.Duration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard JSON interno protegido por autenticación de servicio. */
@RestController
@RequestMapping("/api/internal/demand/v1/observability")
public class DemandObservabilityController {
  private final DemandObservabilityService service;

  public DemandObservabilityController(DemandObservabilityService service) {
    this.service = service;
  }

  /** Devuelve el dashboard agregado para 1..744 horas. */
  @GetMapping("/dashboard")
  public DemandObservabilityDashboard dashboard(@RequestParam(defaultValue = "24") int hours) {
    if (hours < 1 || hours > 744) {
      throw new IllegalArgumentException("hours debe estar entre 1 y 744");
    }
    return service.dashboard(Duration.ofHours(hours));
  }
}
