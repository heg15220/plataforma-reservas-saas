package com.reserly.platform.reviews.service;

import com.reserly.platform.reviews.dto.PublicReviewCollectionResponse;
import com.reserly.platform.reviews.dto.VenueReviewListResponse;
import java.util.UUID;

/** Consultas públicas y privadas de reseñas con límites explícitos. */
public interface ReviewQueryService {

  /** Devuelve el tramo reciente y las métricas de un local cuya publicación ya fue acreditada. */
  PublicReviewCollectionResponse findPublic(UUID venueId);

  /**
   * Devuelve una página del local derivado del propietario autenticado.
   *
   * @throws VenueReviewNotFoundException si la cuenta no tiene un local vigente
   * @throws VenueReviewInvalidPageException si la paginación excede los límites
   */
  VenueReviewListResponse findOwned(UUID ownerUserId, int page, int size);
}
