package com.reserly.platform.statistics.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.statistics.dto.VenueStatisticsResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Contrato privado de métricas para el local autenticado. */
@RequestMapping(path = "/api/venue/me/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueStatisticsController {

  /** Recalcula y devuelve hoy, semana, mes, año o un rango inclusivo de hasta 366 días. */
  @GetMapping
  ResponseEntity<VenueStatisticsResponse> get(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam(defaultValue = "today") String period,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);
}
