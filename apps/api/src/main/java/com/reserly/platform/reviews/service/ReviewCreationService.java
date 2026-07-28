package com.reserly.platform.reviews.service;

import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import java.util.UUID;

/** Caso de uso transaccional que crea una única reseña para una reserva acreditada. */
public interface ReviewCreationService {

  /**
   * Crea una reseña tras revalidar propiedad, estado y finalización de la reserva.
   *
   * @throws ReviewInvalidException si el contenido incumple el contrato
   * @throws ReviewNotEligibleException si no se puede acreditar una reserva pasada válida
   * @throws ReviewAlreadySubmittedException si la reserva ya tiene una reseña
   */
  ReviewCreateResponse create(UUID reservationId, ReviewCreateRequest request);
}
