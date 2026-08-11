package com.reserly.platform.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.persistence.AuditLogDao;
import com.reserly.platform.administration.persistence.AuditLogEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica validación, minimización de metadatos y timestamps deterministas. */
class AuditLogServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-27T18:30:00Z");

  private final AuditLogDao auditLogDao = mock(AuditLogDao.class);
  private final AuditLogService service =
      new AuditLogServiceImpl(auditLogDao, Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void returnSavedAudit() {
    when(auditLogDao.saveAndFlush(any(AuditLogEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void recordsValidatedSnapshotsAndBoundsUntrustedMetadata() {
    String longUserAgent = "x".repeat(550);
    AuditLogEntity saved =
        service.record(
            new AuditLogEntry(
                UUID.randomUUID(),
                "venue_owner",
                " no_show_incident ",
                UUID.randomUUID(),
                " report_no_show ",
                Map.of("reservationStatus", "no_show"),
                Map.of("reservationStatus", "reported"),
                " 203.0.113.20 ",
                longUserAgent));

    assertThat(saved.getEntityType()).isEqualTo("no_show_incident");
    assertThat(saved.getAction()).isEqualTo("report_no_show");
    assertThat(saved.getBeforeJson()).containsEntry("reservationStatus", "no_show");
    assertThat(saved.getAfterJson()).containsEntry("reservationStatus", "reported");
    assertThat(saved.getIpAddress()).isEqualTo("203.0.113.20");
    assertThat(saved.getUserAgent()).hasSize(500);
    assertThat(saved.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void rejectsUnknownActorRoleBeforePersistence() {
    AuditLogEntry invalid =
        new AuditLogEntry(
            UUID.randomUUID(),
            "customer",
            "no_show_incident",
            UUID.randomUUID(),
            "report_no_show",
            null,
            null,
            null,
            null);

    assertThatThrownBy(() -> service.record(invalid)).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(auditLogDao);
  }

  @Test
  void permitsOnlyActorlessSystemEntries() {
    AuditLogEntity saved =
        service.record(
            new AuditLogEntry(
                null,
                "system",
                "payment",
                UUID.randomUUID(),
                "payment.callback_accepted",
                null,
                Map.of("status", "confirmed"),
                null,
                null));

    assertThat(saved.getActorUserId()).isNull();
    assertThat(saved.getActorRole()).isEqualTo("system");
    assertThatThrownBy(
            () ->
                service.record(
                    new AuditLogEntry(
                        UUID.randomUUID(),
                        "system",
                        "payment",
                        UUID.randomUUID(),
                        "payment.callback_accepted",
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
