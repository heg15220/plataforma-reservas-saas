package com.reserly.platform.businessverification.service;

import java.util.UUID;

/** Frontera obligatoria que los casos de uso de publicación deben consultar en backend. */
public interface VenuePublicationEligibilityService {

  /** Evalúa condiciones sin devolver datos empresariales sensibles. */
  VenuePublicationEligibility evaluate(UUID businessAccountId);

  /** Interrumpe la operación si cualquier condición previa de RB-012 está pendiente. */
  void requireEligible(UUID businessAccountId);
}
