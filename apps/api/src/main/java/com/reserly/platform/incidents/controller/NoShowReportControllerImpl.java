package com.reserly.platform.incidents.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.incidents.dto.NoShowReportRequest;
import com.reserly.platform.incidents.dto.NoShowReportResponse;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.incidents.service.NoShowReportAuditContext;
import com.reserly.platform.incidents.service.NoShowReportService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que captura metadatos técnicos sin confiar en cabeceras de proxy. */
@RestController
public class NoShowReportControllerImpl implements NoShowReportController {

  private final NoShowReportService reportService;

  public NoShowReportControllerImpl(NoShowReportService reportService) {
    this.reportService = reportService;
  }

  @Override
  public ResponseEntity<NoShowReportResponse> report(
      AuthenticatedAccount account,
      UUID reservationId,
      NoShowReportRequest request,
      HttpServletRequest servletRequest) {
    NoShowIncidentEntity incident =
        reportService.report(
            account.userId(),
            reservationId,
            request,
            new NoShowReportAuditContext(
                servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
    return ResponseEntity.ok(
        new NoShowReportResponse(
            incident.getId(),
            incident.getReservationId(),
            incident.getStatus(),
            incident.getReportedAt()));
  }
}
