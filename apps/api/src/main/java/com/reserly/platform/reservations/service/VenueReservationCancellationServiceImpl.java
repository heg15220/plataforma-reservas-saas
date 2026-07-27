package com.reserly.platform.reservations.service;

import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.reservations.dto.VenueReservationCancellationRequest;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela una reserva confirmada futura dentro de la misma transacción que su auditoría.
 *
 * <p>La capacidad se libera por el cambio de estado: las consultas de ocupación solo contabilizan
 * estados activos. El email se publica como evento y se encola después del commit.
 */
@Service
public class VenueReservationCancellationServiceImpl
    implements VenueReservationCancellationService {

  private final ReservationDao reservationDao;
  private final AuditLogService auditLogService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public VenueReservationCancellationServiceImpl(
      ReservationDao reservationDao,
      AuditLogService auditLogService,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.reservationDao = reservationDao;
    this.auditLogService = auditLogService;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Override
  @Transactional
  public ReservationEntity cancel(
      UUID ownerUserId,
      UUID reservationId,
      VenueReservationCancellationRequest request,
      VenueReservationCancellationAuditContext auditContext) {
    String reason = requireReason(request);
    if (ownerUserId == null || reservationId == null) {
      throw new VenueReservationCancellationNotFoundException();
    }
    ReservationEntity reservation =
        reservationDao
            .findOwnedForAttendanceUpdate(ownerUserId, reservationId)
            .orElseThrow(VenueReservationCancellationNotFoundException::new);
    Instant now = clock.instant();
    Instant startsAt =
        reservation.getDate().atTime(reservation.getStartsAt()).atZone(clock.getZone()).toInstant();
    if (!"confirmed".equals(reservation.getStatus()) || !startsAt.isAfter(now)) {
      throw new VenueReservationCancellationInvalidException();
    }

    reservation.setStatus("cancelled_by_venue");
    reservation.setCancelledAt(now);
    reservation.setCancelledBy("venue");
    reservation.setCancellationReason(reason);
    reservation.setSecureTokenHash(null);
    reservation.setSecureTokenExpiresAt(null);
    reservation.setUpdatedAt(now);
    ReservationEntity saved = reservationDao.saveAndFlush(reservation);

    auditLogService.record(
        new AuditLogEntry(
            ownerUserId,
            "venue_owner",
            "reservation",
            saved.getId(),
            "cancel_by_venue",
            Map.of("reservationStatus", "confirmed"),
            Map.of(
                "reservationStatus", "cancelled_by_venue",
                "cancelledBy", "venue",
                "cancellationReason", reason),
            auditContext == null ? null : auditContext.ipAddress(),
            auditContext == null ? null : auditContext.userAgent()));
    eventPublisher.publishEvent(emailEvent(saved, reason));
    return saved;
  }

  private VenueReservationCancellationEmailRequestedEvent emailEvent(
      ReservationEntity reservation, String reason) {
    String locale =
        SupportedLocale.fromLanguageTag(reservation.getCustomerLocale())
            .orElseGet(
                () ->
                    SupportedLocale.fromLanguageTag(reservation.getVenue().getDefaultLocale())
                        .orElse(SupportedLocale.EN))
            .languageTag();
    return new VenueReservationCancellationEmailRequestedEvent(
        UUID.randomUUID(),
        reservation.getId(),
        reservation.getCustomerEmail(),
        locale,
        reservation.getVenue().getName(),
        reservation.getVenue().getAddress(),
        reservation.getDate(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getPartySize(),
        reason);
  }

  private String requireReason(VenueReservationCancellationRequest request) {
    if (request == null || request.reason() == null || request.reason().isBlank()) {
      throw new VenueReservationCancellationInvalidException();
    }
    String normalized = request.reason().strip();
    if (normalized.length() > 500) {
      throw new VenueReservationCancellationInvalidException();
    }
    return normalized;
  }
}
