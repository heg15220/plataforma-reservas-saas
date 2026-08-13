package com.reserly.platform.demand.ingestion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica exclusivamente el namespace interno de demanda mediante secreto de servicio.
 *
 * <p>La comparación es constante y el secreto no se registra ni se convierte en principal. La
 * infraestructura debe transportar esta cabecera solo sobre TLS y rotarla desde un gestor seguro.
 */
@Component
public class DemandServiceAuthenticationFilter extends OncePerRequestFilter {

  public static final String TOKEN_HEADER = "X-Reserly-Service-Token";

  private final DemandIngestionProperties properties;

  public DemandServiceAuthenticationFilter(DemandIngestionProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String supplied = request.getHeader(TOKEN_HEADER);
    if (properties.enabled() && constantTimeEquals(supplied, properties.serviceToken())) {
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              properties.serviceId(),
              null,
              List.of(new SimpleGrantedAuthority("ROLE_DEMAND_INGESTOR")));
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
    }
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.equals("/api/internal/demand/v1/events")
        && !path.startsWith("/api/internal/demand/v1/events/");
  }

  private boolean constantTimeEquals(String supplied, String expected) {
    if (supplied == null || expected == null) {
      return false;
    }
    return MessageDigest.isEqual(
        supplied.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
  }
}
