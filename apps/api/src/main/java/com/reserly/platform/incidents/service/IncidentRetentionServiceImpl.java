package com.reserly.platform.incidents.service;

import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Desidentifica el historial al terminar su utilidad operativa y elimina la evidencia al vencer el
 * plazo bloqueado.
 *
 * <p>Las operaciones masivas son condicionales e idempotentes. Se eliminan primero penalizaciones
 * para respetar su clave foránea hacia la incidencia origen; no se leen emails ni notas en Java.
 */
@Service
public class IncidentRetentionServiceImpl implements IncidentRetentionService {

  private final NoShowIncidentDao incidentDao;
  private final PenaltyDao penaltyDao;
  private final AuditLogService auditLogService;
  private final Clock clock;
  private final int operationalMonths;
  private final int evidenceMonths;

  public IncidentRetentionServiceImpl(
      NoShowIncidentDao incidentDao,
      PenaltyDao penaltyDao,
      AuditLogService auditLogService,
      Clock clock,
      @Value("${reserly.incidents.retention.operationalMonths:12}") int operationalMonths,
      @Value("${reserly.incidents.retention.evidenceMonths:36}") int evidenceMonths) {
    if (operationalMonths < 1 || evidenceMonths <= operationalMonths || evidenceMonths > 120) {
      throw new IllegalArgumentException("Invalid incident retention periods");
    }
    this.incidentDao = incidentDao;
    this.penaltyDao = penaltyDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
    this.operationalMonths = operationalMonths;
    this.evidenceMonths = evidenceMonths;
  }

  @Override
  @Transactional
  public IncidentRetentionResult enforceRetention() {
    Instant now = clock.instant();
    Instant operationalCutoff =
        now.atZone(clock.getZone()).minusMonths(operationalMonths).toInstant();
    Instant evidenceCutoff = now.atZone(clock.getZone()).minusMonths(evidenceMonths).toInstant();
    int incidentsAnonymized = incidentDao.anonymizeOperationalHistory(operationalCutoff, now);
    int penaltiesAnonymized = penaltyDao.anonymizeOperationalHistory(operationalCutoff, now);
    int penaltiesDeleted = penaltyDao.deleteExpiredEvidence(evidenceCutoff);
    int incidentsDeleted = incidentDao.deleteExpiredEvidence(evidenceCutoff);
    IncidentRetentionResult result =
        new IncidentRetentionResult(
            incidentsAnonymized, penaltiesAnonymized, penaltiesDeleted, incidentsDeleted);
    if (result.changed()) {
      auditLogService.record(
          new AuditLogEntry(
              null,
              "system",
              "incident_retention_cycle",
              cycleId(now),
              "incident_retention.enforced",
              null,
              Map.of(
                  "operationalCutoff", operationalCutoff.toString(),
                  "evidenceCutoff", evidenceCutoff.toString(),
                  "incidentsAnonymized", incidentsAnonymized,
                  "penaltiesAnonymized", penaltiesAnonymized,
                  "penaltiesDeleted", penaltiesDeleted,
                  "incidentsDeleted", incidentsDeleted),
              null,
              null));
    }
    return result;
  }

  private UUID cycleId(Instant now) {
    return UUID.nameUUIDFromBytes(("incident-retention:" + now).getBytes(StandardCharsets.UTF_8));
  }
}
