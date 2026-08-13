package com.reserly.platform.demand.retention;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara lotes periódicos; la observabilidad consume contadores sin registrar sujetos. */
@Component
public class DemandRetentionJob {
  private final DemandRetentionService service;

  public DemandRetentionJob(DemandRetentionService service) {
    this.service = service;
  }

  @Scheduled(cron = "${reserly.demand.retention.cron:0 35 3 * * *}", zone = "UTC")
  public void purgeExpiredData() {
    service.runOnce();
  }
}
