package com.reserly.platform.demand.governance;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Convierte rechazos internos en códigos opacos sin reflejar payload, campo ni valor. */
@RestControllerAdvice(assignableTypes = DemandGovernanceAuditController.class)
public class DemandGovernanceAuditExceptionHandler {

  /** Un contrato inválido no revela la regla concreta que lo bloqueó. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> invalidContract() {
    return ResponseEntity.badRequest().body(Map.of("error", "GOVERNANCE_AUDIT_INVALID"));
  }

  /** Distingue un replay divergente de otros fallos de dominio sin exponer su contenido. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalidEvent(IllegalArgumentException exception) {
    boolean conflict = "DEMAND_GOVERNANCE_EVENT_ID_CONFLICT".equals(exception.getMessage());
    return ResponseEntity.status(conflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST)
        .body(Map.of("error", conflict ? "GOVERNANCE_AUDIT_CONFLICT" : "GOVERNANCE_AUDIT_INVALID"));
  }
}
