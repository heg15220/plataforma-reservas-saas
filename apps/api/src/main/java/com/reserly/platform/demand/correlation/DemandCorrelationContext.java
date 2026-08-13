package com.reserly.platform.demand.correlation;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Resuelve el identificador validado por el filtro sin depender de valores de negocio o PII. */
@Component
public class DemandCorrelationContext {

  /** Cabecera pública de correlación, no usada para autenticación ni autorización. */
  public static final String HEADER_NAME = "X-Reserly-Correlation-Id";

  static final String REQUEST_ATTRIBUTE = DemandCorrelationContext.class.getName() + ".requestId";

  /**
   * Devuelve la correlación del request actual o un UUID nuevo para ejecuciones sin contexto HTTP.
   */
  public UUID currentOrNew() {
    if (RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes) {
      HttpServletRequest request = attributes.getRequest();
      Object value = request.getAttribute(REQUEST_ATTRIBUTE);
      if (value instanceof UUID requestId) {
        return requestId;
      }
    }
    return UUID.randomUUID();
  }
}
