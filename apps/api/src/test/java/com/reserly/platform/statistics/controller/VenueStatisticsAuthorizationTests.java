package com.reserly.platform.statistics.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.identity.security.RestAccessDeniedHandler;
import com.reserly.platform.identity.security.RestAuthenticationEntryPoint;
import com.reserly.platform.statistics.dto.VenueStatisticsResponse;
import com.reserly.platform.statistics.service.VenueStatisticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Acredita la política privada del endpoint sin iniciar Spring Boot ni persistencia. */
@ExtendWith(MockitoExtension.class)
class VenueStatisticsAuthorizationTests {

  @Mock private VenueStatisticsService service;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new VenueStatisticsControllerImpl(service))
            .setControllerAdvice(new VenueStatisticsExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .apply(springSecurity(securityFilter()))
            .build();
  }

  @Test
  void rejectsAnonymousAndAdminBeforeStatisticsService() throws Exception {
    mockMvc
        .perform(get("/api/venue/me/statistics"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    mockMvc
        .perform(
            get("/api/venue/me/statistics")
                .with(authentication(account(principal(AccountType.ADMIN), "ROLE_ADMIN"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));

    verifyNoInteractions(service);
  }

  @Test
  void permitsVenueOwnerAndUsesPrincipalIdentity() throws Exception {
    AuthenticatedAccount principal = principal(AccountType.VENUE_BUSINESS);
    UUID venueId = UUID.randomUUID();
    when(service.findOwned(principal.userId(), venueId, "month", null, null))
        .thenReturn(emptyResponse());

    mockMvc
        .perform(
            get("/api/venue/me/statistics")
                .queryParam("period", "month")
                .queryParam("venueId", venueId.toString())
                .with(authentication(account(principal, "ROLE_VENUE_OWNER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.period").value("month"))
        .andExpect(jsonPath("$.series").isArray());

    verify(service).findOwned(principal.userId(), venueId, "month", null, null);
  }

  private VenueStatisticsResponse emptyResponse() {
    return new VenueStatisticsResponse(
        "month",
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 7, 29),
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        new BigDecimal("0.0"),
        0,
        0,
        null,
        List.of());
  }

  private FilterChainProxy securityFilter() {
    var authorization =
        RequestMatcherDelegatingAuthorizationManager.builder()
            .add(
                PathPatternRequestMatcher.pathPattern("/api/venue/me/**"),
                AuthorityAuthorizationManager.hasRole("VENUE_OWNER"))
            .add(
                AnyRequestMatcher.INSTANCE,
                (authentication, context) -> new AuthorizationDecision(true))
            .build();
    var exceptionTranslation = new ExceptionTranslationFilter(new RestAuthenticationEntryPoint());
    exceptionTranslation.setAccessDeniedHandler(new RestAccessDeniedHandler());
    var chain =
        new DefaultSecurityFilterChain(
            AnyRequestMatcher.INSTANCE,
            new SecurityContextHolderFilter(new RequestAttributeSecurityContextRepository()),
            exceptionTranslation,
            new AuthorizationFilter(authorization));
    return new FilterChainProxy(chain);
  }

  private UsernamePasswordAuthenticationToken account(
      AuthenticatedAccount principal, String authority) {
    return UsernamePasswordAuthenticationToken.authenticated(
        principal, null, Set.of(new SimpleGrantedAuthority(authority)));
  }

  private AuthenticatedAccount principal(AccountType accountType) {
    String role = accountType == AccountType.ADMIN ? "admin" : "venue_owner";
    return new AuthenticatedAccount(
        UUID.randomUUID(), UUID.randomUUID(), accountType, "es", Set.of(role));
  }
}
