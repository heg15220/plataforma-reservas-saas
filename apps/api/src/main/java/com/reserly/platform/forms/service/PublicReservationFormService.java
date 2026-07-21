package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.PublicReservationFormResponse;

/** Consulta el esquema público sin exponer borradores ni campos desactivados. */
public interface PublicReservationFormService {
  PublicReservationFormResponse findPublishedByVenueSlug(String venueSlug);
}