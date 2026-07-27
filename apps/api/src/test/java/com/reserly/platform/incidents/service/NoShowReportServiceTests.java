package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.incidents.dto.NoShowReportRequest;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

/** Verifica confirmación, propiedad, transición y auditoría minimizada del reporte. */
class NoShowReportServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-27T18:30:00Z");

  private final ReservationDao reservationDao = mock(ReservationDao.class);
  private final NoShowIncidentDao incidentDao = mock(NoShowIncidentDao.class);
  private final AuditLogService auditLogService = mock(AuditLogService.class);
  private final NoShowReportService service =
      new NoShowReportServiceImpl(
          reservationDao,
          incidentDao,
          auditLogService,
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void returnPersistedEntities() {
    when(incidentDao.saveAndFlush(any(NoShowIncidentEntity.class)))
        .thenAnswer(
            invocation -> {
              NoShowIncidentEntity incident = invocation.getArgument(0);
              incident.setId(UUID.randomUUID());
              return incident;
            });
    when(reservationDao.saveAndFlush(any(ReservationEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void reportsMarkedNoShowAndRecordsMinimizedAuditInSameUseCase() {
    UUID ownerId = UUID.randomUUID();
    ReservationEntity reservation = noShowReservation();
    when(reservationDao.findOwnedForAttendanceUpdate(ownerId, reservation.getId()))
        .thenReturn(Optional.of(reservation));

    NoShowIncidentEntity incident =
        service.report(
            ownerId,
            reservation.getId(),
            new NoShowReportRequest(true, "  El cliente no acudió.  "),
            new NoShowReportAuditContext("203.0.113.10", "Browser/1.0"));

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getVenueId()).isEqualTo(reservation.getVenue().getId());
    assertThat(incident.getReservationId()).isEqualTo(reservation.getId());
    assertThat(incident.getCustomerEmailNormalized()).isEqualTo("user@example.com");
    assertThat(incident.getIncidentType()).isEqualTo("no_show");
    assertThat(incident.getReportedByUserId()).isEqualTo(ownerId);
    assertThat(incident.getReportedAt()).isEqualTo(NOW);
    assertThat(incident.getNotes()).isEqualTo("El cliente no acudió.");
    assertThat(incident.getStatus()).isEqualTo("reported");
    assertThat(reservation.getStatus()).isEqualTo("reported");
    assertThat(reservation.getUpdatedAt()).isEqualTo(NOW);

    ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(auditLogService).record(auditCaptor.capture());
    AuditLogEntry audit = auditCaptor.getValue();
    assertThat(audit.actorUserId()).isEqualTo(ownerId);
    assertThat(audit.actorRole()).isEqualTo("venue_owner");
    assertThat(audit.entityType()).isEqualTo("no_show_incident");
    assertThat(audit.entityId()).isEqualTo(incident.getId());
    assertThat(audit.action()).isEqualTo("report_no_show");
    assertThat(audit.beforeJson()).containsEntry("reservationStatus", "no_show");
    assertThat(audit.afterJson())
        .containsEntry("reservationStatus", "reported")
        .containsEntry("incidentStatus", "reported");
    assertThat(audit.beforeJson()).doesNotContainKeys("email", "notes");
    assertThat(audit.afterJson()).doesNotContainKeys("email", "notes");
    assertThat(audit.ipAddress()).isEqualTo("203.0.113.10");
  }

  @Test
  void requiresExplicitConfirmationBeforeDatabaseAccess() {
    assertThatThrownBy(
            () ->
                service.report(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new NoShowReportRequest(false, null),
                    null))
        .isInstanceOf(NoShowReportInvalidException.class);

    verifyNoInteractions(reservationDao);
    verifyNoInteractions(auditLogService);
  }

  @Test
  void rejectsReservationThatWasNotMarkedNoShowWithoutSideEffects() {
    UUID ownerId = UUID.randomUUID();
    ReservationEntity reservation = noShowReservation();
    reservation.setStatus("attended");
    when(reservationDao.findOwnedForAttendanceUpdate(ownerId, reservation.getId()))
        .thenReturn(Optional.of(reservation));

    assertThatThrownBy(
            () ->
                service.report(
                    ownerId,
                    reservation.getId(),
                    new NoShowReportRequest(true, null),
                    null))
        .isInstanceOf(NoShowReportStateException.class);

    verify(incidentDao, never()).saveAndFlush(any());
    verify(reservationDao, never()).saveAndFlush(any());
    verifyNoInteractions(auditLogService);
  }

  @Test
  void keepsForeignAndMissingReservationOpaque() {
    UUID ownerId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    when(reservationDao.findOwnedForAttendanceUpdate(ownerId, reservationId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.report(
                    ownerId,
                    reservationId,
                    new NoShowReportRequest(true, null),
                    null))
        .isInstanceOf(NoShowReportNotFoundException.class);
  }

  @Test
  void reportAndAuditParticipateInTransactionalBoundary() throws NoSuchMethodException {
    Transactional transactional =
        NoShowReportServiceImpl.class
            .getMethod(
                "report",
                UUID.class,
                UUID.class,
                NoShowReportRequest.class,
                NoShowReportAuditContext.class)
            .getAnnotation(Transactional.class);

    assertThat(transactional).isNotNull();
  }

  private ReservationEntity noShowReservation() {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(UUID.randomUUID());
    reservation.setVenue(venue);
    reservation.setCustomerEmail("user@example.com");
    reservation.setCustomerEmailNormalized("user@example.com");
    reservation.setStatus("no_show");
    return reservation;
  }
}
