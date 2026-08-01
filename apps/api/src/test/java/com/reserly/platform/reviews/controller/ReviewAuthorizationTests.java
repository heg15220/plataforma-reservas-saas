package com.reserly.platform.reviews.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.identity.security.RestAccessDeniedHandler;
import com.reserly.platform.identity.security.RestAuthenticationEntryPoint;
import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import com.reserly.platform.reviews.dto.VenueReviewListResponse;
import com.reserly.platform.reviews.service.ReviewCreationService;
import com.reserly.platform.reviews.service.ReviewQueryService;
import java.math.BigDecimal;
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

/**
 * Verifica la frontera de autorización de reseñas sin arrancar persistencia ni infraestructura.
 *
 * <p>Reproduce la política central: la creación bajo {@code /api/public/**} admite al usuario
 * anónimo y revalida email/reserva en el servicio, mientras {@code /api/venue/me/**} exige {@code
 * ROLE_VENUE_OWNER} y deriva el propietario exclusivamente del principal.
 */
@ExtendWith(MockitoExtension.class)
class ReviewAuthorizationTests {

  @Mock private ReviewCreationService creationService;
  @Mock private ReviewQueryService queryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ReviewCreationControllerImpl(creationService),
                new VenueReviewControllerImpl(queryService))
            .setControllerAdvice(new ReviewExceptionHandler(), new VenueReviewExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .apply(springSecurity(securityFilter()))
            .build();
  }

  @Test
  void permitsAnonymousCreationAndDelegatesEligibilityToTheService() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();
    when(creationService.create(
            reservationId, new ReviewCreateRequest("guest@example.com", 5, "Excelente.", true)))
        .thenReturn(
            new ReviewCreateResponse(
                "created", reviewId, venueId, reservationId, 5, new BigDecimal("4.8"), 12));

    mockMvc
        .perform(
            post("/api/public/reservations/{reservationId}/reviews", reservationId)
                .contentType("application/json")
                .content(
                    """
                    {
                      "customerEmail": "guest@example.com",
                      "rating": 5,
                      "comment": "Excelente.",
                      "acceptsReviewPolicy": true
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reviewId").value(reviewId.toString()));

    verify(creationService)
        .create(reservationId, new ReviewCreateRequest("guest@example.com", 5, "Excelente.", true));
  }

  @Test
  void rejectsAnonymousAndAdminFromPrivateReviewsBeforeQuerying() throws Exception {
    mockMvc
        .perform(get("/api/venue/me/reviews"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    mockMvc
        .perform(
            get("/api/venue/me/reviews")
                .with(authentication(account(principal(AccountType.ADMIN), "ROLE_ADMIN"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));

    verifyNoInteractions(queryService);
  }

  @Test
  void permitsVenueOwnerAndUsesOnlyPrincipalIdentity() throws Exception {
    AuthenticatedAccount principal = principal(AccountType.VENUE_BUSINESS);
    when(queryService.findOwned(principal.userId(), 0, 20))
        .thenReturn(new VenueReviewListResponse(null, 0, List.of(), 0, 20, 0));

    mockMvc
        .perform(
            get("/api/venue/me/reviews")
                .with(authentication(account(principal, "ROLE_VENUE_OWNER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewsCount").value(0));

    verify(queryService).findOwned(principal.userId(), 0, 20);
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
