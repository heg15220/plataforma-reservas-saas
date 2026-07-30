package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminPlanListResponse;
import com.reserly.platform.administration.dto.AdminPlanRequest;
import com.reserly.platform.administration.dto.AdminPlanResponse;
import java.util.UUID;

/** Gestión localizada y auditada del catálogo SaaS. */
public interface AdminPlanService {
  AdminPlanListResponse list();

  AdminPlanResponse create(UUID actorUserId, AdminPlanRequest request, AdminRequestContext context);

  AdminPlanResponse update(
      UUID actorUserId, UUID planId, AdminPlanRequest request, AdminRequestContext context);
}
