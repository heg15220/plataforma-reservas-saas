package com.reserly.platform.demand.observability;

/** Rechazos de vida del proceso por código opaco y de baja cardinalidad. */
public record DemandRejectionMetric(String code, long count) {}
