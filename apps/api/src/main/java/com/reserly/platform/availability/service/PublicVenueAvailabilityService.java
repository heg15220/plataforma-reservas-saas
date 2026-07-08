package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import com.reserly.platform.localization.SupportedLocale;
import java.time.LocalDate;

/** Caso de uso anónimo para consultar disponibilidad real publicada de un local. */
public interface PublicVenueAvailabilityService {

  /** Calcula estado operativo y franjas públicas para un slug publicado y una fecha concreta. */
  PublicVenueAvailabilityResponse findBySlug(String slug, LocalDate date, SupportedLocale locale);
}
