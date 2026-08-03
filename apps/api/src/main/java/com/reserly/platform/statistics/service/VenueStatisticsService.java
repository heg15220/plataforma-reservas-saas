package com.reserly.platform.statistics.service;

import com.reserly.platform.statistics.dto.VenueStatisticsResponse;
import java.time.LocalDate;
import java.util.UUID;

/** Consulta privada de estadísticas derivada exclusivamente del propietario autenticado. */
public interface VenueStatisticsService {

  /**
   * Recalcula y devuelve el periodo solicitado.
   *
   * @param userId identidad obtenida de la sesión
   * @param venueId local seleccionado o {@code null} para el perfil compatible predeterminado
   * @param period filtro canónico o {@code null} para hoy
   * @param fromDate inicio inclusivo obligatorio solo para custom
   * @param toDate final inclusivo obligatorio solo para custom
   */
  VenueStatisticsResponse findOwned(
      UUID userId, UUID venueId, String period, LocalDate fromDate, LocalDate toDate);
}
