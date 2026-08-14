package com.reserly.platform.demand.observability;

import com.reserly.platform.demand.quality.DemandDatasetQualityReport;
import java.time.Instant;
import java.util.List;

/** Read model interno para volumen persistido, resultados runtime, latencia y cobertura. */
public record DemandObservabilityDashboard(
    Instant generatedAt,
    Instant windowStart,
    String runtimeCounterScope,
    long totalPersistedVolume,
    double instrumentationCoveragePercent,
    List<String> missingEventTypes,
    List<DemandEventMetric> events,
    List<DemandRejectionMetric> rejectionReasons,
    DemandDatasetQualityReport quality) {}
