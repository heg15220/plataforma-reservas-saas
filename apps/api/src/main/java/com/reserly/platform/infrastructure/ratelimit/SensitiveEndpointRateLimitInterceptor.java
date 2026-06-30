package com.reserly.platform.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Aplica cuotas por dirección observada directamente por el servidor a endpoints anónimos.
 *
 * <p>No confía en {@code X-Forwarded-For}, que un cliente podría falsificar. En despliegues con
 * proxy, la capa de entrada debe normalizar la dirección remota mediante infraestructura de
 * confianza antes de que la petición llegue a la aplicación.
 */
@Component
public class SensitiveEndpointRateLimitInterceptor implements HandlerInterceptor {

  private static final Map<String, RateLimitScope> PROTECTED_POST_PATHS =
      Map.of(
          "/api/auth/login", RateLimitScope.LOGIN,
          "/api/auth/venues/register", RateLimitScope.REGISTRATION,
          "/api/auth/password/forgot", RateLimitScope.PASSWORD_RESET_REQUEST,
          "/api/auth/password/reset", RateLimitScope.PASSWORD_RESET_CONSUME);

  private final RateLimitService rateLimitService;

  public SensitiveEndpointRateLimitInterceptor(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!"POST".equals(request.getMethod())) {
      return true;
    }
    RateLimitScope scope = PROTECTED_POST_PATHS.get(request.getRequestURI());
    if (scope != null) {
      String remoteAddress = request.getRemoteAddr();
      rateLimitService.check(scope, remoteAddress == null ? "unknown" : remoteAddress);
    }
    return true;
  }
}
