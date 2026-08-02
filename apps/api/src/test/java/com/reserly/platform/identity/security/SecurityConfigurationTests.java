package com.reserly.platform.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/** Protege la lista exacta de orígenes permitidos para las peticiones con cookie de sesión. */
class SecurityConfigurationTests {

  @Test
  void exposesEveryConfiguredLocalOriginWithoutUsingAWildcard() {
    List<URI> origins =
        List.of(URI.create("http://localhost:3000"), URI.create("http://localhost:3001"));
    ReserlyProperties properties =
        new ReserlyProperties(
            ReserlyEnvironment.LOCAL,
            URI.create("http://localhost:8080"),
            URI.create("http://localhost:3000"),
            origins,
            new ReserlyProperties.Security(false),
            new ReserlyProperties.Features(false));
    CorsConfigurationSource source =
        new SecurityConfiguration().corsConfigurationSource(properties);
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/venue/me");

    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins())
        .containsExactly("http://localhost:3000", "http://localhost:3001")
        .doesNotContain("*");
    assertThat(configuration.getAllowCredentials()).isTrue();
  }
}
