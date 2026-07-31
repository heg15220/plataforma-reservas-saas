package com.reserly.platform.administration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.administration.dto.AdminCategoryRequest;
import com.reserly.platform.administration.dto.AdminCategoryResponse;
import com.reserly.platform.administration.dto.AdminMetricsResponse;
import com.reserly.platform.administration.service.AdminAuditQueryService;
import com.reserly.platform.administration.service.AdminBusinessAccountService;
import com.reserly.platform.administration.service.AdminCategoryService;
import com.reserly.platform.administration.service.AdminDocumentService;
import com.reserly.platform.administration.service.AdminIncidentService;
import com.reserly.platform.administration.service.AdminMetricsService;
import com.reserly.platform.administration.service.AdminPenaltyService;
import com.reserly.platform.administration.service.AdminPlanService;
import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.administration.service.AdminVenueService;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.identity.security.RestAccessDeniedHandler;
import com.reserly.platform.identity.security.RestAuthenticationEntryPoint;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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

/** Acredita que lecturas y mutaciones administrativas exigen el rol persistido {@code admin}. */
@ExtendWith(MockitoExtension.class)
class AdminCatalogAuthorizationTests {

  private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");
  private static final Instant NOW = Instant.parse("2026-07-31T08:00:00Z");

  @Mock private AdminCategoryService categoryService;
  @Mock private AdminVenueService venueService;
  @Mock private AdminIncidentService incidentService;
  @Mock private AdminBusinessAccountService businessAccountService;
  @Mock private AdminDocumentService documentService;
  @Mock private AdminPenaltyService penaltyService;
  @Mock private AdminPlanService planService;
  @Mock private AdminMetricsService metricsService;
  @Mock private AdminAuditQueryService auditQueryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller =
        new AdminCatalogControllerImpl(
            categoryService,
            venueService,
            incidentService,
            businessAccountService,
            documentService,
            penaltyService,
            planService,
            metricsService,
            auditQueryService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new AdminCatalogExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .apply(springSecurity(securityFilter()))
            .build();
  }

  @Test
  void rejectsAnonymousAndVenueOwnerBeforeAnyAdminService() throws Exception {
    mockMvc
        .perform(get("/api/admin/metrics"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));

    mockMvc
        .perform(
            get("/api/admin/metrics")
                .with(
                    authentication(
                        account(principal(AccountType.VENUE_BUSINESS), "ROLE_VENUE_OWNER"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));

    mockMvc
        .perform(
            post("/api/admin/categories")
                .with(
                    authentication(
                        account(principal(AccountType.VENUE_BUSINESS), "ROLE_VENUE_OWNER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"slug":"restricted","nameEs":"Restringida","nameEn":"Restricted","active":true}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));

    verifyNoInteractions(
        categoryService,
        venueService,
        incidentService,
        businessAccountService,
        documentService,
        penaltyService,
        planService,
        metricsService,
        auditQueryService);
  }

  @Test
  void permitsAdminToReadAggregatedMetrics() throws Exception {
    when(metricsService.snapshot())
        .thenReturn(new AdminMetricsResponse(5, 3, 1, 20, 14, 4, 2, 3, 1, NOW));

    mockMvc
        .perform(
            get("/api/admin/metrics")
                .with(authentication(account(principal(AccountType.ADMIN), "ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalVenues").value(5))
        .andExpect(jsonPath("$.pendingBusinessReviews").value(2));

    verify(metricsService).snapshot();
  }

  @Test
  void derivesMutationActorAndRequestContextFromAuthenticatedRequest() throws Exception {
    UUID categoryId = UUID.randomUUID();
    when(categoryService.create(
            eq(ACTOR_ID), any(AdminCategoryRequest.class), any(AdminRequestContext.class)))
        .thenReturn(
            new AdminCategoryResponse(categoryId, "wellness", "Bienestar", "Wellness", true, NOW));

    mockMvc
        .perform(
            post("/api/admin/categories")
                .with(authentication(account(principal(AccountType.ADMIN), "ROLE_ADMIN")))
                .with(
                    request -> {
                      request.setRemoteAddr("198.51.100.10");
                      return request;
                    })
                .header("User-Agent", "admin-security-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"slug":"wellness","nameEs":"Bienestar","nameEn":"Wellness","active":true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(categoryId.toString()));

    verify(categoryService)
        .create(
            eq(ACTOR_ID),
            eq(new AdminCategoryRequest("wellness", "Bienestar", "Wellness", true)),
            eq(new AdminRequestContext("198.51.100.10", "admin-security-test")));
  }

  private FilterChainProxy securityFilter() {
    var authorization =
        RequestMatcherDelegatingAuthorizationManager.builder()
            .add(
                PathPatternRequestMatcher.pathPattern("/api/admin/**"),
                AuthorityAuthorizationManager.hasRole("ADMIN"))
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
    UUID userId = accountType == AccountType.ADMIN ? ACTOR_ID : UUID.randomUUID();
    return new AuthenticatedAccount(userId, UUID.randomUUID(), accountType, "es", Set.of(role));
  }
}
