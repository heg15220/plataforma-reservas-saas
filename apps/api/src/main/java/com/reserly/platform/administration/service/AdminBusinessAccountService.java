package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminBusinessAccountListResponse;
import com.reserly.platform.administration.dto.AdminBusinessAccountResponse;
import com.reserly.platform.administration.dto.AdminBusinessDecisionRequest;
import com.reserly.platform.administration.dto.AdminBusinessRecheckRequest;
import java.util.UUID;

/** Consulta administrativa de identidades empresariales pendientes, sin resolverlas. */
public interface AdminBusinessAccountService {
  AdminBusinessAccountListResponse listPending();

  AdminBusinessAccountResponse getPending(UUID accountId);

  AdminBusinessAccountResponse decide(
      UUID actorUserId,
      UUID accountId,
      AdminBusinessDecisionRequest request,
      AdminRequestContext context);

  AdminBusinessAccountResponse recheck(
      UUID actorUserId,
      UUID accountId,
      AdminBusinessRecheckRequest request,
      AdminRequestContext context);
}
