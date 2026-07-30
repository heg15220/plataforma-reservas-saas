package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminIncidentListResponse;
import com.reserly.platform.administration.dto.AdminIncidentResponse;
import com.reserly.platform.administration.dto.AdminIncidentReviewRequest;
import java.util.UUID;

/** Lectura y resolución auditada de incidencias bajo rol administrador. */
public interface AdminIncidentService {
  AdminIncidentListResponse list();

  AdminIncidentResponse review(
      UUID actorUserId, UUID incidentId, AdminIncidentReviewRequest request,
      AdminRequestContext context);
}
