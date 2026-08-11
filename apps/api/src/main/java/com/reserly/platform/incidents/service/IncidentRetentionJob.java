package com.reserly.platform.incidents.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Programa la conservación diaria y registra únicamente contadores agregados. */
@Component
public class IncidentRetentionJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(IncidentRetentionJob.class);
  private final IncidentRetentionService retentionService;

  public IncidentRetentionJob(IncidentRetentionService retentionService) {
    this.retentionService = retentionService;
  }

  /** Ejecuta el ciclo diario configurable; repetirlo sin datos vencidos no produce escrituras. */
  @Scheduled(
      cron = "${reserly.incidents.retention.cron:0 45 2 * * *}",
      zone = "${reserly.business-clock.zone-id:Europe/Madrid}")
  public IncidentRetentionResult enforceRetention() {
    IncidentRetentionResult result = retentionService.enforceRetention();
    if (result.changed()) {
      LOGGER.info(
          "Incident retention enforced: incidentsAnonymized={}, penaltiesAnonymized={}, "
              + "penaltiesDeleted={}, incidentsDeleted={}",
          result.incidentsAnonymized(),
          result.penaltiesAnonymized(),
          result.penaltiesDeleted(),
          result.incidentsDeleted());
    }
    return result;
  }
}
