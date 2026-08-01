package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminPenaltyListResponse;
import com.reserly.platform.administration.dto.AdminPenaltyResponse;
import com.reserly.platform.administration.dto.AdminPenaltyUpdateRequest;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import com.reserly.platform.incidents.persistence.PenaltyEntity;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Permite revocar o ajustar el fin de una restricción activa, nunca crearla ni reactivarla. */
@Service
public class AdminPenaltyServiceImpl implements AdminPenaltyService {
  static final int LIST_LIMIT = 100;
  private final PenaltyDao penaltyDao;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public AdminPenaltyServiceImpl(
      PenaltyDao penaltyDao, AuditLogService auditLogService, Clock clock) {
    this.penaltyDao = penaltyDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminPenaltyListResponse list() {
    return new AdminPenaltyListResponse(
        penaltyDao.findAdminPage(PageRequest.of(0, LIST_LIMIT)).stream()
            .map(this::response)
            .toList());
  }

  @Override
  @Transactional
  public AdminPenaltyResponse update(
      UUID actorUserId,
      UUID penaltyId,
      AdminPenaltyUpdateRequest request,
      AdminRequestContext context) {
    PenaltyEntity penalty =
        penaltyDao
            .findByIdForAdminUpdate(penaltyId)
            .orElseThrow(AdminResourceNotFoundException::new);
    if (!"active".equals(penalty.getStatus())) {
      throw new AdminResourceConflictException();
    }
    Map<String, Object> before = snapshot(penalty);
    if ("revoked".equals(request.status())) {
      penalty.setStatus("revoked");
    } else {
      if (request.endsAt() == null || !request.endsAt().isAfter(penalty.getStartsAt())) {
        throw new AdminResourceConflictException();
      }
      penalty.setEndsAt(request.endsAt());
    }
    penalty.setUpdatedAt(clock.instant());
    penaltyDao.saveAndFlush(penalty);
    Map<String, Object> after = new LinkedHashMap<>(snapshot(penalty));
    after.put("reason", request.reason().strip());
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "penalty",
            penalty.getId(),
            "penalty.admin_updated",
            before,
            after,
            context.ipAddress(),
            context.userAgent()));
    return response(penalty);
  }

  private Map<String, Object> snapshot(PenaltyEntity penalty) {
    return Map.of("status", penalty.getStatus(), "endsAt", penalty.getEndsAt().toString());
  }

  private AdminPenaltyResponse response(PenaltyEntity penalty) {
    return new AdminPenaltyResponse(
        penalty.getId(),
        penalty.getCustomerEmailNormalized(),
        penalty.getScope(),
        penalty.getVenueId(),
        penalty.getIncidentCountOperational(),
        penalty.getStartsAt(),
        penalty.getEndsAt(),
        penalty.getStatus(),
        penalty.getReason(),
        penalty.getCreatedFromIncidentId(),
        penalty.getUpdatedAt());
  }
}
