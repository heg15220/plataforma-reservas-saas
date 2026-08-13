package com.reserly.platform.demand.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** Verifica que solo el secreto exacto autentica el namespace interno. */
class DemandServiceAuthenticationFilterTests {

  private static final String TOKEN = "test-demand-token-at-least-32-characters";

  @Test
  void authenticatesExactTokenAndIgnoresInvalidToken() throws Exception {
    DemandServiceAuthenticationFilter filter =
        new DemandServiceAuthenticationFilter(
            new DemandIngestionProperties(true, "test-producer", TOKEN, 100, Duration.ofDays(90)));
    MockHttpServletRequest accepted =
        new MockHttpServletRequest("POST", "/api/internal/demand/v1/events");
    accepted.addHeader(DemandServiceAuthenticationFilter.TOKEN_HEADER, TOKEN);
    filter.doFilter(accepted, new MockHttpServletResponse(), new MockFilterChain());
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo("test-producer");
    SecurityContextHolder.clearContext();

    MockHttpServletRequest rejected =
        new MockHttpServletRequest("POST", "/api/internal/demand/v1/events");
    rejected.addHeader(
        DemandServiceAuthenticationFilter.TOKEN_HEADER, "wrong-token-value-that-is-long-enough");
    filter.doFilter(rejected, new MockHttpServletResponse(), new MockFilterChain());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
