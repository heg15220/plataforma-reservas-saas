package com.reserly.platform.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import com.reserly.platform.identity.controller.SessionCookieFactory;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Verifica la frontera CSRF stateless sin arrancar Spring ni infraestructura externa. */
class BrowserCsrfProtectionFilterTests {

  private BrowserCsrfProtectionFilter filter;

  @BeforeEach
  void setUp() {
    ReserlyProperties properties =
        new ReserlyProperties(
            ReserlyEnvironment.TEST,
            URI.create("http://localhost:8080"),
            URI.create("http://localhost:3000"),
            List.of(URI.create("http://localhost:3000"), URI.create("http://localhost:3001")),
            new ReserlyProperties.Security(false),
            new ReserlyProperties.Features(false));
    filter = new BrowserCsrfProtectionFilter(properties);
  }

  @Test
  void acceptsConfiguredWebOriginAndApiOriginForAuthenticatedWrites() throws Exception {
    assertAccepted(request("POST", "/api/venue/me/profile", "http://localhost:3000", null));
    assertAccepted(request("DELETE", "/api/admin/venues/id", "http://localhost:8080", null));
  }

  @Test
  void acceptsTrustedRefererWhenOriginIsUnavailable() throws Exception {
    assertAccepted(
        request(
            "POST", "/api/auth/logout", null, "http://localhost:3001/panel/reservas?day=today"));
  }

  @Test
  void rejectsMissingNullMalformedAndUntrustedOriginsWithoutContinuing() throws Exception {
    assertRejected(request("POST", "/api/venue/me/profile", null, null));
    assertRejected(request("POST", "/api/venue/me/profile", "null", null));
    assertRejected(request("POST", "/api/venue/me/profile", "https://attacker.example", null));
    assertRejected(
        request("POST", "/api/venue/me/profile", "http://localhost:3000/forged-path", null));
  }

  @Test
  void leavesSafeAnonymousAndNonSessionPublicRequestsOutsideCsrfBoundary() throws Exception {
    assertAccepted(request("GET", "/api/venue/me", "https://attacker.example", null));

    MockHttpServletRequest anonymousWrite = new MockHttpServletRequest("POST", "/api/venue/me");
    assertAccepted(anonymousWrite);

    assertAccepted(
        request("POST", "/api/public/reservations/holds", "https://attacker.example", null));
  }

  private MockHttpServletRequest request(
      String method, String path, String origin, String referer) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setCookies(new Cookie(SessionCookieFactory.COOKIE_NAME, "session-secret"));
    if (origin != null) {
      request.addHeader("Origin", origin);
    }
    if (referer != null) {
      request.addHeader("Referer", referer);
    }
    return request;
  }

  private void assertAccepted(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  private void assertRejected(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"CSRF_VALIDATION_FAILED\"}");
  }
}
