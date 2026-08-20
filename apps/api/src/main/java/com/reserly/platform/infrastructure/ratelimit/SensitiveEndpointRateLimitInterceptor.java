package com.reserly.platform.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.regex.Pattern;
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
  private static final Pattern RESERVATION_CONFIRM =
      Pattern.compile("^/api/public/reservations/[0-9a-fA-F-]{36}/confirm$");
  private static final Pattern RESERVATION_REVIEW =
      Pattern.compile("^/api/public/reservations/[0-9a-fA-F-]{36}/reviews$");
  private static final Pattern VENUE_REVIEW =
      Pattern.compile("^/api/public/venues/[a-z0-9]+(?:-[a-z0-9]+)*/reviews(?:/eligibility)?$");
  private static final Pattern PUBLIC_MANAGEMENT_LINK =
      Pattern.compile("^/api/public/reservations/manage/[A-Za-z0-9_-]{43}(?:/cancel)?$");
  private static final Pattern WAITLIST_OFFER_ACCEPT =
      Pattern.compile("^/api/public/waitlist/offers/[A-Za-z0-9_-]{43}/accept$");

  private final RateLimitService rateLimitService;

  public SensitiveEndpointRateLimitInterceptor(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    RateLimitScope scope = resolveScope(request.getMethod(), request.getRequestURI());
    if (scope != null) {
      String remoteAddress = request.getRemoteAddr();
      rateLimitService.check(scope, remoteAddress == null ? "unknown" : remoteAddress);
    }
    return true;
  }

  /** Clasifica únicamente operaciones anónimas con coste o secreto, sin leer payloads ni tokens. */
  private RateLimitScope resolveScope(String method, String path) {
    if ("POST".equals(method)) {
      RateLimitScope exact = PROTECTED_POST_PATHS.get(path);
      if (exact != null) {
        return exact;
      }
      if ("/api/public/reservations/holds".equals(path)
          || RESERVATION_CONFIRM.matcher(path).matches()
          || WAITLIST_OFFER_ACCEPT.matcher(path).matches()) {
        return RateLimitScope.RESERVATION;
      }
      if (RESERVATION_REVIEW.matcher(path).matches() || VENUE_REVIEW.matcher(path).matches()) {
        return RateLimitScope.REVIEW;
      }
    }
    if (("GET".equals(method) || "POST".equals(method))
        && PUBLIC_MANAGEMENT_LINK.matcher(path).matches()) {
      return RateLimitScope.PUBLIC_LINK;
    }
    return null;
  }
}
