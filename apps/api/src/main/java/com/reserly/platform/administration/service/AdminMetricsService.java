package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminMetricsResponse;

/** Snapshot agregado de métricas globales iniciales. */
public interface AdminMetricsService {
  AdminMetricsResponse snapshot();
}
