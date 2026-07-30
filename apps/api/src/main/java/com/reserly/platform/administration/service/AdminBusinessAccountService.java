package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminBusinessAccountListResponse;
import com.reserly.platform.administration.dto.AdminBusinessAccountResponse;
import java.util.UUID;

/** Consulta administrativa de identidades empresariales pendientes, sin resolverlas. */
public interface AdminBusinessAccountService {
  AdminBusinessAccountListResponse listPending();

  AdminBusinessAccountResponse getPending(UUID accountId);
}
