package com.reserly.platform.reviews.service;

import com.reserly.platform.reviews.dto.ReviewEligibilityRequest;
import com.reserly.platform.reviews.dto.ReviewEligibilityResponse;

/** Caso de uso público y minimizado de elegibilidad por local publicado y email normalizado. */
public interface ReviewEligibilityService {

  /**
   * Decide si existe una visita pasada sin reseña, sin exponer ningún dato de esa visita.
   *
   * @throws ReviewInvalidException si slug o email incumplen el contrato
   */
  ReviewEligibilityResponse check(String venueSlug, ReviewEligibilityRequest request);
}
