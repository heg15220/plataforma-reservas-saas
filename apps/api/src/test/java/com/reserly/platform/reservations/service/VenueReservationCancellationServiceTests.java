package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.reservations.dto.VenueReservationCancellationRequest;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/** Cubre propiedad, futuro, motivo, auditoría atómica y evento post-commit. */
class VenueReservationCancellationServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-27T10:00:00Z");

  private final ReservationDao reservationDao = mock(ReservationDao.class);
  private final AuditLogService auditLogService = mock(AuditLogService.class);
  private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
  private final VenueReservationCancellationService service =
      new VenueReservationCancellationServiceImpl(
          reservationDao, auditLogService, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void returnSavedReservation() {
    when(reservationDao.saveAndFlush(any(ReservationEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void cancelsFutureOwnedReservationAndAuditsReasonBeforePublishingEmail() {
    UUID ownerId = UUID.randomUUID();
    ReservationEntity reservation = reservation();
    when(reservationDao.findOwnedForAttendanceUpdate(ownerId, reservation.getId()))
        .thenReturn(Optional.of(reservation));

    ReservationEntity result =
        service.cancel(
            ownerId,
            reservation.getId(),
            new VenueReservationCancellationRequest("  Cierre operativo imprevisto.  "),
            new VenueReservationCancellationAuditContext("203.0.113.5", "Browser/1.0"));

    assertThat(result.getStatus()).isEqualTo("cancelled_by_venue");
    assertThat(result.getCancelledAt()).isEqualTo(NOW);
    assertThat(result.getCancelledBy()).isEqualTo("venue");
    assertThat(result.getCancellationReason()).isEqualTo("Cierre operativo imprevisto.");
    assertThat(result.getSecureTokenHash()).isNull();
    assertThat(result.getSecureTokenExpiresAt()).isNull();

    ArgumentCaptor<AuditLogEntry> audit = ArgumentCaptor.forClass(AuditLogEntry.class);
    verify(auditLogService).record(audit.capture());
    assertThat(audit.getValue().actorUserId()).isEqualTo(ownerId);
    assertThat(audit.getValue().actorRole()).isEqualTo("venue_owner");
    assertThat(audit.getValue().entityType()).isEqualTo("reservation");
    assertThat(audit.getValue().entityId()).isEqualTo(reservation.getId());
    assertThat(audit.getValue().action()).isEqualTo("cancel_by_venue");
    assertThat(audit.getValue().beforeJson())
        .containsOnlyKeys("reservationStatus")
        .containsEntry("reservationStatus", "confirmed");
    assertThat(audit.getValue().afterJson())
        .containsOnlyKeys("reservationStatus", "cancelledBy", "cancellationReason")
        .containsEntry("reservationStatus", "cancelled_by_venue")
        .containsEntry("cancelledBy", "venue")
        .containsEntry("cancellationReason", "Cierre operativo imprevisto.");
    assertThat(audit.getValue().afterJson()).doesNotContainKeys("email", "customerName");
    assertThat(audit.getValue().ipAddress()).isEqualTo("203.0.113.5");
    assertThat(audit.getValue().userAgent()).isEqualTo("Browser/1.0");

    ArgumentCaptor<VenueReservationCancellationEmailRequestedEvent> event =
        ArgumentCaptor.forClass(VenueReservationCancellationEmailRequestedEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().customerEmail()).isEqualTo("ana@example.com");
    assertThat(event.getValue().customerLocale()).isEqualTo("es");
    assertThat(event.getValue().cancellationReason()).isEqualTo("Cierre operativo imprevisto.");

    var ordered = inOrder(reservationDao, auditLogService, eventPublisher);
    ordered.verify(reservationDao).saveAndFlush(reservation);
    ordered.verify(auditLogService).record(audit.getValue());
    ordered.verify(eventPublisher).publishEvent(event.getValue());
  }

  @Test
  void rejectsMissingReasonBeforeDatabaseAndPastOrRepeatedReservationWithoutEffects() {
    assertThatThrownBy(
            () ->
                service.cancel(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new VenueReservationCancellationRequest(" "),
                    null))
        .isInstanceOf(VenueReservationCancellationInvalidException.class);
    verifyNoInteractions(reservationDao);

    UUID ownerId = UUID.randomUUID();
    ReservationEntity past = reservation();
    past.setDate(LocalDate.of(2026, 7, 26));
    when(reservationDao.findOwnedForAttendanceUpdate(ownerId, past.getId()))
        .thenReturn(Optional.of(past));
    assertThatThrownBy(
            () ->
                service.cancel(
                    ownerId, past.getId(), new VenueReservationCancellationRequest("Motivo"), null))
        .isInstanceOf(VenueReservationCancellationInvalidException.class);
    verify(reservationDao, never()).saveAndFlush(past);
    verifyNoInteractions(auditLogService);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void keepsForeignReservationOpaque() {
    UUID ownerId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    when(reservationDao.findOwnedForAttendanceUpdate(ownerId, reservationId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.cancel(
                    ownerId,
                    reservationId,
                    new VenueReservationCancellationRequest("Motivo"),
                    null))
        .isInstanceOf(VenueReservationCancellationNotFoundException.class);
  }

  @Test
  void cancellationAndAuditShareTheTransactionalBoundary() throws NoSuchMethodException {
    Transactional transactional =
        VenueReservationCancellationServiceImpl.class
            .getMethod(
                "cancel",
                UUID.class,
                UUID.class,
                VenueReservationCancellationRequest.class,
                VenueReservationCancellationAuditContext.class)
            .getAnnotation(Transactional.class);

    assertThat(transactional).isNotNull();
  }

  private ReservationEntity reservation() {
    UserEntity owner = new UserEntity();
    owner.setId(UUID.randomUUID());
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setOwnerUser(owner);
    venue.setName("Local");
    venue.setAddress("Calle Mayor 1");
    venue.setDefaultLocale("en");
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(UUID.randomUUID());
    reservation.setVenue(venue);
    reservation.setCustomerName("Ana");
    reservation.setCustomerEmail("ana@example.com");
    reservation.setCustomerEmailNormalized("ana@example.com");
    reservation.setCustomerLocale("es");
    reservation.setPartySize(2);
    reservation.setDate(LocalDate.of(2026, 7, 28));
    reservation.setStartsAt(LocalTime.of(10, 0));
    reservation.setEndsAt(LocalTime.of(11, 0));
    reservation.setStatus("confirmed");
    reservation.setSecureTokenHash("a".repeat(64));
    reservation.setSecureTokenExpiresAt(NOW.plusSeconds(86_400));
    reservation.setCreatedAt(NOW.minusSeconds(86_400));
    reservation.setUpdatedAt(NOW.minusSeconds(60));
    return reservation;
  }
}
