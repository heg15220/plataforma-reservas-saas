package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifica fronteras, orden referencial, idempotencia y auditoría agregada de conservación. */
class IncidentRetentionServiceTests {

  private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");

  @Test
  void anonymizesThenDeletesInReferentialOrderAndAuditsCounts() {
    NoShowIncidentDao incidentDao = mock(NoShowIncidentDao.class);
    PenaltyDao penaltyDao = mock(PenaltyDao.class);
    AuditLogService auditLogService = mock(AuditLogService.class);
    Instant operationalCutoff = Instant.parse("2025-08-11T12:00:00Z");
    Instant evidenceCutoff = Instant.parse("2023-08-11T12:00:00Z");
    when(incidentDao.anonymizeOperationalHistory(operationalCutoff, NOW)).thenReturn(3);
    when(penaltyDao.anonymizeOperationalHistory(operationalCutoff, NOW)).thenReturn(2);
    when(penaltyDao.deleteExpiredEvidence(evidenceCutoff)).thenReturn(1);
    when(incidentDao.deleteExpiredEvidence(evidenceCutoff)).thenReturn(1);
    IncidentRetentionService service =
        new IncidentRetentionServiceImpl(
            incidentDao, penaltyDao, auditLogService, Clock.fixed(NOW, ZoneOffset.UTC), 12, 36);

    IncidentRetentionResult result = service.enforceRetention();

    assertThat(result).isEqualTo(new IncidentRetentionResult(3, 2, 1, 1));
    var ordered = inOrder(incidentDao, penaltyDao);
    ordered.verify(incidentDao).anonymizeOperationalHistory(operationalCutoff, NOW);
    ordered.verify(penaltyDao).anonymizeOperationalHistory(operationalCutoff, NOW);
    ordered.verify(penaltyDao).deleteExpiredEvidence(evidenceCutoff);
    ordered.verify(incidentDao).deleteExpiredEvidence(evidenceCutoff);
    verify(auditLogService).record(any());
  }

  @Test
  void unchangedCycleProducesNoAuditAndInvalidPeriodsFailFast() {
    NoShowIncidentDao incidentDao = mock(NoShowIncidentDao.class);
    PenaltyDao penaltyDao = mock(PenaltyDao.class);
    AuditLogService auditLogService = mock(AuditLogService.class);
    IncidentRetentionService service =
        new IncidentRetentionServiceImpl(
            incidentDao, penaltyDao, auditLogService, Clock.fixed(NOW, ZoneOffset.UTC), 12, 36);

    assertThat(service.enforceRetention().changed()).isFalse();
    verify(auditLogService, never()).record(any());
    assertThatThrownBy(
            () ->
                new IncidentRetentionServiceImpl(
                    incidentDao,
                    penaltyDao,
                    auditLogService,
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    12,
                    12))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
