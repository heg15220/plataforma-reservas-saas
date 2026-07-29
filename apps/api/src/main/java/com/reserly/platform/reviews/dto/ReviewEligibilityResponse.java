package com.reserly.platform.reviews.dto;

/**
 * Decisión pública cerrada que no contiene identificadores, fechas ni número de visitas.
 *
 * @param eligible indica que existe una reserva pasada válida todavía sin reseña
 * @param canReview réplica explícita para el contrato de interfaz
 * @param error código estable o {@code null} cuando es elegible
 * @param messageKey clave pública i18n o {@code null} cuando es elegible
 */
public record ReviewEligibilityResponse(
    boolean eligible, boolean canReview, String error, String messageKey) {

  public static ReviewEligibilityResponse allowed() {
    return new ReviewEligibilityResponse(true, true, null, null);
  }

  public static ReviewEligibilityResponse notEligible() {
    return new ReviewEligibilityResponse(
        false, false, "REVIEW_NOT_ELIGIBLE", "reviews.notEligibleForVenue");
  }

  public static ReviewEligibilityResponse alreadySubmitted() {
    return new ReviewEligibilityResponse(
        false, false, "REVIEW_ALREADY_SUBMITTED", "reviews.alreadySubmittedForVenue");
  }
}
