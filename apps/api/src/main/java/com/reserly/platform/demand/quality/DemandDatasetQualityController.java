package com.reserly.platform.demand.quality;

import java.time.Duration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Frontera interna autenticada para ejecutar auditorías agregadas bajo demanda. */
@RestController
@RequestMapping("/api/internal/demand/v1/quality")
public class DemandDatasetQualityController {
  private final DemandDatasetQualityService service;

  public DemandDatasetQualityController(DemandDatasetQualityService service) {
    this.service = service;
  }

  /** Devuelve contadores de las últimas horas; nunca ejemplos ni identificadores afectados. */
  @GetMapping
  public DemandDatasetQualityReport audit(@RequestParam(defaultValue = "24") int hours) {
    if (hours < 1 || hours > 744) {
      throw new IllegalArgumentException("hours debe estar entre 1 y 744");
    }
    return service.audit(Duration.ofHours(hours));
  }
}
