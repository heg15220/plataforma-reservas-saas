package com.reserly.platform.venues.service;

import java.util.Set;

/** Rechazo de publicación con motivos no sensibles aptos para orientar al panel. */
public class VenuePublicationRejectedException extends RuntimeException {

  private final Set<VenuePublicationRequirement> requirements;

  public VenuePublicationRejectedException(Set<VenuePublicationRequirement> requirements) {
    super("El local todavía no cumple los requisitos de publicación.");
    this.requirements = Set.copyOf(requirements);
  }

  public Set<VenuePublicationRequirement> getRequirements() {
    return requirements;
  }
}
