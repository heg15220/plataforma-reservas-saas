package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminIncidentListResponse;
import com.reserly.platform.administration.dto.AdminIncidentResponse;
import com.reserly.platform.administration.dto.AdminIncidentReviewRequest;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa una cola acotada y decisiones que no alteran reservas ni penalizaciones. */
@Service
public class AdminIncidentServiceImpl implements AdminIncidentService {
  static final int LIST_LIMIT = 100;
  private final NoShowIncidentDao incidentDao;
  private final VenueDao venueDao;
  private final AuditLogService auditLogService;

  public AdminIncidentServiceImpl(
      NoShowIncidentDao incidentDao, VenueDao venueDao, AuditLogService auditLogService) {
    this.incidentDao = incidentDao;
    this.venueDao = venueDao;
    this.auditLogService = auditLogService;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminIncidentListResponse list() {
    var incidents = incidentDao.findAdminPage(PageRequest.of(0, LIST_LIMIT));
    var venues =
        venueDao
            .findAllById(incidents.stream().map(NoShowIncidentEntity::getVenueId).toList())
            .stream()
            .collect(Collectors.toMap(VenueEntity::getId, Function.identity()));
    return new AdminIncidentListResponse(
        incidents.stream().map(item -> response(item, venues.get(item.getVenueId()))).toList());
  }

  @Override
  @Transactional
  public AdminIncidentResponse review(
      UUID actorUserId,
      UUID incidentId,
      AdminIncidentReviewRequest request,
      AdminRequestContext context) {
    NoShowIncidentEntity incident =
        incidentDao
            .findByIdForAdminReview(incidentId)
            .orElseThrow(AdminResourceNotFoundException::new);
    if (!"reported".equals(incident.getStatus())) {
      throw new AdminResourceConflictException();
    }
    String beforeStatus = incident.getStatus();
    incident.setStatus(request.status());
    incidentDao.saveAndFlush(incident);
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "incident",
            incident.getId(),
            "incident.reviewed",
            Map.of("status", beforeStatus),
            Map.of("status", incident.getStatus(), "reason", request.reason().strip()),
            context.ipAddress(),
            context.userAgent()));
    VenueEntity venue =
        venueDao.findById(incident.getVenueId()).orElseThrow(AdminResourceNotFoundException::new);
    return response(incident, venue);
  }

  private AdminIncidentResponse response(NoShowIncidentEntity incident, VenueEntity venue) {
    return new AdminIncidentResponse(
        incident.getId(),
        incident.getReservationId(),
        incident.getVenueId(),
        venue == null ? null : venue.getName(),
        incident.getCustomerEmailNormalized(),
        incident.getIncidentType(),
        incident.getReportedByUserId(),
        incident.getReportedAt(),
        incident.getNotes(),
        incident.getStatus());
  }
}
