package com.reserly.platform.billing.dto;

import java.time.Instant;
import java.util.List;

/**
 * Resumen privado de suscripción y catálogo contratable.
 *
 * <p>No contiene IDs de local, suscripción, plan o pago. Cuando aún no existe una suscripción
 * materializada, el plan gratuito se presenta como estado efectivo activo.
 *
 * @param currentPlan plan efectivo
 * @param subscriptionStatus estado canónico
 * @param billingPeriod periodicidad canónica
 * @param renewalAt siguiente renovación o {@code null} si no aplica
 * @param trialEndsAt fin de prueba o {@code null}
 * @param cancelledAt cancelación o {@code null}
 * @param monetization estado operativo de cobros
 * @param availablePlans catálogo activo localizado
 */
public record VenueSubscriptionResponse(
    SubscriptionPlanResponse currentPlan,
    String subscriptionStatus,
    String billingPeriod,
    Instant renewalAt,
    Instant trialEndsAt,
    Instant cancelledAt,
    MonetizationStatusResponse monetization,
    List<SubscriptionPlanResponse> availablePlans) {

  /** Impide mutar el catálogo después de construir la respuesta. */
  public VenueSubscriptionResponse {
    availablePlans = List.copyOf(availablePlans);
  }
}
