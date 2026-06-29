package com.reserly.platform.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import com.reserly.platform.identity.service.SessionProperties;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class SessionCookieFactoryTests {

  private static final SessionProperties SESSION_PROPERTIES =
      new SessionProperties(Duration.ofHours(12));

  @Test
  void createsHostOnlyHttpOnlyStrictCookieAndUsesEnvironmentSecureFlag() {
    ResponseCookie production =
        new SessionCookieFactory(
                properties(ReserlyEnvironment.PRODUCTION, true), SESSION_PROPERTIES)
            .create("token");
    ResponseCookie local =
        new SessionCookieFactory(properties(ReserlyEnvironment.LOCAL, false), SESSION_PROPERTIES)
            .create("token");

    assertThat(production.getName()).isEqualTo(SessionCookieFactory.COOKIE_NAME);
    assertThat(production.isHttpOnly()).isTrue();
    assertThat(production.isSecure()).isTrue();
    assertThat(production.getSameSite()).isEqualTo("Strict");
    assertThat(production.getPath()).isEqualTo("/");
    assertThat(production.getDomain()).isNull();
    assertThat(production.getMaxAge()).isEqualTo(Duration.ofHours(12));
    assertThat(local.isSecure()).isFalse();
  }

  @Test
  void clearsCookieWithSameScopeAndZeroMaxAge() {
    ResponseCookie cleared =
        new SessionCookieFactory(properties(ReserlyEnvironment.LOCAL, false), SESSION_PROPERTIES)
            .clear();

    assertThat(cleared.getValue()).isEmpty();
    assertThat(cleared.isHttpOnly()).isTrue();
    assertThat(cleared.getSameSite()).isEqualTo("Strict");
    assertThat(cleared.getPath()).isEqualTo("/");
    assertThat(cleared.getMaxAge()).isEqualTo(Duration.ZERO);
  }

  private ReserlyProperties properties(ReserlyEnvironment environment, boolean secureCookies) {
    String scheme = environment == ReserlyEnvironment.LOCAL ? "http" : "https";
    URI api = URI.create(scheme + "://api.reserly.example");
    URI web = URI.create(scheme + "://reserly.example");
    return new ReserlyProperties(
        environment,
        api,
        web,
        List.of(web),
        new ReserlyProperties.Security(secureCookies),
        new ReserlyProperties.Features(false));
  }
}
