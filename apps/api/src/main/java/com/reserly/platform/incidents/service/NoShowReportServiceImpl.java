package com.reserly.platform.incidents.service;

import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.incidents.dto.NoShowReportRequest;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación transaccional del reporte profesional de no asistencia.
 *
 * <p>La incidencia, la transición de reserva, la auditoría y la penalización global comparten la
 * misma transacción. Cualquier fallo revierte el reporte completo.
 */
@Service
public class NoShowReportServiceImpl implements NoShowReportService {

  private static final String INCIDENT_TYPE = "no_show";
  private static final String INCIDENT_STATUS = "reported";

  private final ReservationDao reservationDao;
  private final NoShowIncidentDao incidentDao;
  private final AuditLogService auditLogService;
  private final PenaltyService penaltyService;
  private final Clock clock;

  public NoShowReportServiceImpl(
      ReservationDao reservationDao,
      NoShowIncidentDao incidentDao,
      AuditLogService auditLogService,
      PenaltyService penaltyService,
      Clock clock) {
    this.reservationDao = reservationDao;
    this.incidentDao = incidentDao;
    this.auditLogService = auditLogService;
    this.penaltyService = penaltyService;
    this.clock = clock;
  }

  @Override
  @Transactional
  public NoShowIncidentEntity report(
      UUID ownerUserId,
      UUID reservationId,
      NoShowReportRequest request,
      NoShowReportAuditContext auditContext) {
    requireConfirmedRequest(request);
    if (ownerUserId == null || reservationId == null) {
      throw new NoShowReportNotFoundException();
    }

    ReservationEntity reservation =
        reservationDao
            .findOwnedForAttendanceUpdate(ownerUserId, reservationId)
            .orElseThrow(NoShowReportNotFoundException::new);
    if (!"no_show".equals(reservation.getStatus())) {
      throw new NoShowReportStateException();
    }
    if (reservation.getCustomerEmailNormalized() == null
        || reservation.getCustomerEmailNormalized().isBlank()) {
      throw new NoShowReportInvalidException();
    }

    Instant now = clock.instant();
    NoShowIncidentEntity incident = new NoShowIncidentEntity();
    incident.setVenueId(reservation.getVenue().getId());
    incident.setReservationId(reservation.getId());
    incident.setCustomerEmailNormalized(reservation.getCustomerEmailNormalized());
    incident.setIncidentType(INCIDENT_TYPE);
    incident.setReportedByUserId(ownerUserId);
    incident.setReportedAt(now);
    incident.setNotes(normalizeNotes(request.notes()));
    incident.setStatus(INCIDENT_STATUS);
    incident.setCreatedAt(now);
    NoShowIncidentEntity savedIncident = incidentDao.saveAndFlush(incident);

    reservation.setStatus("reported");
    reservation.setUpdatedAt(now);
    reservationDao.saveAndFlush(reservation);

    auditLogService.record(
        new AuditLogEntry(
            ownerUserId,
            "venue_owner",
            "no_show_incident",
            savedIncident.getId(),
            "report_no_show",
            Map.of("reservationStatus", "no_show"),
            Map.of(
                "reservationStatus", "reported",
                "incidentStatus", INCIDENT_STATUS,
                "incidentType", INCIDENT_TYPE),
            auditContext == null ? null : auditContext.ipAddress(),
            auditContext == null ? null : auditContext.userAgent()));
    penaltyService.applyFor(savedIncident);
    return savedIncident;
  }

  private void requireConfirmedRequest(NoShowReportRequest request) {
    if (request == null || !request.confirmed()) {
      throw new NoShowReportInvalidException();
    }
  }

  private String normalizeNotes(String notes) {
    if (notes == null || notes.isBlank()) {
      return null;
    }
    String normalized = notes.trim();
    if (normalized.length() > 2000) {
      throw new NoShowReportInvalidException();
    }
    return normalized;
  }
}
