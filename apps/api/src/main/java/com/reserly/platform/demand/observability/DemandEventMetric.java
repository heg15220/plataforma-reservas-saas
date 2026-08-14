package com.reserly.platform.demand.observability;

/** Fila de dashboard por tipo y versión; contadores runtime no contienen dimensiones personales. */
public record DemandEventMetric(
    String eventType,
    short schemaVersion,
    long persistedVolume,
    long accepted,
    long rejected,
    long duplicates,
    long latencySamples,
    double meanLatencyMilliseconds,
    double maximumLatencyMilliseconds,
    boolean covered) {}
