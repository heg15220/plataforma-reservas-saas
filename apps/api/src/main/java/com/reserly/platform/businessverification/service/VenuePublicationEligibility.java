package com.reserly.platform.businessverification.service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Decisión backend sobre las condiciones empresariales previas a publicar.
 *
 * <p>No contiene email, identificador fiscal ni evidencia de verificación.
 */
public record VenuePublicationEligibility(Set<VenuePublicationBlocker> blockers) {

  public VenuePublicationEligibility {
    Objects.requireNonNull(blockers);
    blockers =
        blockers.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(blockers));
  }

  /** Solo una decisión sin bloqueos permite continuar con las reglas de perfil de la Fase 2. */
  public boolean allowed() {
    return blockers.isEmpty();
  }
}
