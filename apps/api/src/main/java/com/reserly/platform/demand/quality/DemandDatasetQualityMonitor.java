package com.reserly.platform.demand.quality;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Publica gauges sin etiquetas de alta cardinalidad a partir de la auditoría agregada. */
@Component
public class DemandDatasetQualityMonitor {
  private final DemandDatasetQualityService service;
  private final Map<Dimension, AtomicLong> values = new EnumMap<>(Dimension.class);

  public DemandDatasetQualityMonitor(DemandDatasetQualityService service, MeterRegistry registry) {
    this.service = service;
    for (Dimension dimension : Dimension.values()) {
      AtomicLong value = new AtomicLong();
      values.put(dimension, value);
      Gauge.builder("reserly.demand.dataset.quality", value, AtomicLong::get)
          .tag("dimension", dimension.tag)
          .register(registry);
    }
  }

  /** Recalcula la ventana móvil; un error conserva la última lectura y lo gestiona Spring. */
  @Scheduled(cron = "${reserly.demand.quality.cron:0 15 * * * *}", zone = "UTC")
  public void refresh() {
    DemandDatasetQualityReport report = service.audit(Duration.ofHours(24));
    values.get(Dimension.INCOMPLETE).set(report.incompleteEvents());
    values.get(Dimension.DUPLICATE).set(report.duplicateEvents());
    values.get(Dimension.TEMPORAL).set(report.temporalOrderViolations());
    values.get(Dimension.CONSENT).set(report.consentViolations());
    values.get(Dimension.PII).set(report.piiLeakageEvents());
  }

  private enum Dimension {
    INCOMPLETE("incomplete"),
    DUPLICATE("duplicate"),
    TEMPORAL("temporal_order"),
    CONSENT("consent"),
    PII("pii_leakage");

    private final String tag;

    Dimension(String tag) {
      this.tag = tag;
    }
  }
}
