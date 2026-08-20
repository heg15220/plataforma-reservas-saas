package com.reserly.platform.demand.waitlist.dto;

import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/** Preferencia opcional de profesional; local, servicio, franja y tamaño proceden de la oferta. */
public record WaitlistOfferAcceptanceRequest(
    UUID employeeResourceId,
    @Pattern(regexp = "^(any_available|specific)$") String assignmentPreference) {}
