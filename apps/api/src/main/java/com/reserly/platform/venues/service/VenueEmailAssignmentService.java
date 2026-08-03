package com.reserly.platform.venues.service;

import com.reserly.platform.venues.dto.VenueEmailAssignmentResponse;
import java.util.List;
import java.util.UUID;

/** Gestiona destinatarios operativos por local publicado dentro del propietario autenticado. */
public interface VenueEmailAssignmentService {

  /** Lista únicamente locales publicados propios, ordenados de forma estable. */
  List<VenueEmailAssignmentResponse> list(UUID ownerUserId);

  /** Sustituye el destinatario y crea o rota la credencial privada de un local publicado propio. */
  VenueEmailAssignmentResponse update(
      UUID ownerUserId, UUID venueId, String email, String rawPassword);
}
