package com.reserly.platform.demand.quality;

import java.time.Instant;

/** Resultado agregado de una auditoría; nunca incluye IDs, contexto ni muestras de eventos. */
public record DemandDatasetQualityReport(
    Instant evaluatedAt,
    Instant windowStart,
    long totalEvents,
    long incompleteEvents,
    long duplicateEvents,
    long temporalOrderViolations,
    long consentViolations,
    long piiLeakageEvents) {

  /** Un dataset es apto solo si no contiene ninguna infracción fundacional. */
  public boolean valid() {
    return incompleteEvents == 0
        && duplicateEvents == 0
        && temporalOrderViolations == 0
        && consentViolations == 0
        && piiLeakageEvents == 0;
  }
}
