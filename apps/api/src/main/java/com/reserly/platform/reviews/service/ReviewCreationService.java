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

  /**
   * Selecciona la visita pasada sin reseña más reciente del email en un local publicado y crea la
   * reseña bajo lock, repitiendo la validación aunque exista una comprobación previa.
   *
   * @throws ReviewInvalidException si el contenido incumple el contrato
   * @throws ReviewNotEligibleException si no existe ninguna visita pasada válida
   * @throws ReviewAlreadySubmittedException si todas las visitas válidas ya fueron reseñadas
   */
  ReviewCreateResponse createForVenue(String venueSlug, ReviewCreateRequest request);
}
