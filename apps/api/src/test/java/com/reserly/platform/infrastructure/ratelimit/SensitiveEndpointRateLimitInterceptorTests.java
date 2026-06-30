package com.reserly.platform.infrastructure.ratelimit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/** Verifica el enrutado de cuotas sin depender del contenido sensible de los payloads. */
class SensitiveEndpointRateLimitInterceptorTests {

  private final RateLimitService rateLimitService = mock(RateLimitService.class);
  private final SensitiveEndpointRateLimitInterceptor interceptor =
      new SensitiveEndpointRateLimitInterceptor(rateLimitService);
  private final HttpServletResponse response = mock(HttpServletResponse.class);

  @Test
  void protectsEveryAnonymousSensitivePostWithObservedRemoteAddress() {
    assertProtected("/api/auth/login", RateLimitScope.LOGIN);
    assertProtected("/api/auth/venues/register", RateLimitScope.REGISTRATION);
    assertProtected("/api/auth/password/forgot", RateLimitScope.PASSWORD_RESET_REQUEST);
    assertProtected("/api/auth/password/reset", RateLimitScope.PASSWORD_RESET_CONSUME);
  }

  @Test
  void ignoresUnmappedPathsAndNonPostMethods() {
    HttpServletRequest unmapped = request("POST", "/api/health");
    HttpServletRequest getLogin = request("GET", "/api/auth/login");

    interceptor.preHandle(unmapped, response, new Object());
    interceptor.preHandle(getLogin, response, new Object());

    verify(rateLimitService, never())
        .check(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  private void assertProtected(String path, RateLimitScope scope) {
    interceptor.preHandle(request("POST", path), response, new Object());
    verify(rateLimitService).check(scope, "203.0.113.9");
  }

  private HttpServletRequest request(String method, String path) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(method);
    when(request.getRequestURI()).thenReturn(path);
    when(request.getRemoteAddr()).thenReturn("203.0.113.9");
    return request;
  }
}
