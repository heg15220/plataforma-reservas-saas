package com.reserly.platform.incidents.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.incidents.dto.IncidentHistoryResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Contrato privado de historial profesional acreditado mediante una reserva propia. */
public interface IncidentHistoryController {

  /**
   * Devuelve como máximo 50 incidencias por página y nunca acepta el email como parámetro.
   *
   * @param reservationId reserva propia usada exclusivamente para derivar la identidad
   */
  @GetMapping(path = "/api/venue/me/incident-history")
  ResponseEntity<IncidentHistoryResponse> find(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam UUID reservationId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size);
}
