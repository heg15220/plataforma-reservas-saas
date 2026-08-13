package com.reserly.platform.demand.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Normaliza correlación UUID para API pública y nunca confía en ella como identidad o permiso.
 *
 * <p>Una cabecera ausente o malformada se reemplaza, evitando reflejar texto arbitrario en
 * respuesta, eventos, métricas o logs. El UUID validado queda como atributo tipado para la
 * telemetría backend.
 */
@Component
public class DemandCorrelationFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/public/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    UUID requestId = parseOrCreate(request.getHeader(DemandCorrelationContext.HEADER_NAME));
    request.setAttribute(DemandCorrelationContext.REQUEST_ATTRIBUTE, requestId);
    response.setHeader(DemandCorrelationContext.HEADER_NAME, requestId.toString());
    filterChain.doFilter(request, response);
  }

  private UUID parseOrCreate(String supplied) {
    if (supplied != null && supplied.length() == 36) {
      try {
        return UUID.fromString(supplied);
      } catch (IllegalArgumentException ignored) {
        // Se reemplaza por un UUID local; nunca se refleja el valor no confiable.
      }
    }
    return UUID.randomUUID();
  }
}
