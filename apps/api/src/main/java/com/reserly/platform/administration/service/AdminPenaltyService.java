package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminPenaltyListResponse;
import com.reserly.platform.administration.dto.AdminPenaltyResponse;
import com.reserly.platform.administration.dto.AdminPenaltyUpdateRequest;
import java.util.UUID;

/** Consulta y modificación auditada de penalizaciones administrativas. */
public interface AdminPenaltyService {
  AdminPenaltyListResponse list();

  AdminPenaltyResponse update(
      UUID actorUserId,
      UUID penaltyId,
      AdminPenaltyUpdateRequest request,
      AdminRequestContext context);
}
