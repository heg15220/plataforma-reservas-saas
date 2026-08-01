package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminVenueListResponse;
import com.reserly.platform.administration.dto.AdminVenueResponse;
import com.reserly.platform.administration.dto.AdminVenueSuspensionRequest;
import com.reserly.platform.administration.dto.AdminVenueUpdateRequest;
import java.util.UUID;

/** Listado y edición básica de locales para administradores. */
public interface AdminVenueService {
  AdminVenueListResponse list();

  AdminVenueResponse update(
      UUID actorUserId, UUID venueId, AdminVenueUpdateRequest request, AdminRequestContext context);

  /** Suspende un local con motivo obligatorio, sin cambiar su cuenta propietaria. */
  AdminVenueResponse suspend(
      UUID actorUserId,
      UUID venueId,
      AdminVenueSuspensionRequest request,
      AdminRequestContext context);
}
