package com.reserly.platform.demand.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Verifica normalización UUID y ausencia de reflejo de valores arbitrarios. */
class DemandCorrelationFilterTests {

  private final DemandCorrelationFilter filter = new DemandCorrelationFilter();

  @Test
  void preservesAValidPublicCorrelation() throws Exception {
    UUID supplied = UUID.randomUUID();
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/public/reservations/holds");
    request.addHeader(DemandCorrelationContext.HEADER_NAME, supplied.toString());
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(request.getAttribute(DemandCorrelationContext.REQUEST_ATTRIBUTE))
        .isEqualTo(supplied);
    assertThat(response.getHeader(DemandCorrelationContext.HEADER_NAME))
        .isEqualTo(supplied.toString());
  }

  @Test
  void replacesInvalidValuesAndSkipsNonPublicNamespaces() throws Exception {
    MockHttpServletRequest publicRequest =
        new MockHttpServletRequest("GET", "/api/public/venues/example");
    publicRequest.addHeader(DemandCorrelationContext.HEADER_NAME, "attacker-controlled-value");
    MockHttpServletResponse publicResponse = new MockHttpServletResponse();

    filter.doFilter(publicRequest, publicResponse, new MockFilterChain());

    String generated = publicResponse.getHeader(DemandCorrelationContext.HEADER_NAME);
    assertThat(UUID.fromString(generated)).isNotNull();
    assertThat(generated).doesNotContain("attacker");

    MockHttpServletRequest privateRequest = new MockHttpServletRequest("GET", "/api/venue/me");
    MockHttpServletResponse privateResponse = new MockHttpServletResponse();
    filter.doFilter(privateRequest, privateResponse, new MockFilterChain());
    assertThat(privateResponse.getHeader(DemandCorrelationContext.HEADER_NAME)).isNull();
  }
}
