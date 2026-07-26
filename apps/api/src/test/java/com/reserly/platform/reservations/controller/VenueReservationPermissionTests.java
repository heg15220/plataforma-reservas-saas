package com.reserly.platform.reservations.controller;

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
import com.reserly.platform.reservations.converter.VenueReservationConverter;
import com.reserly.platform.reservations.service.VenueReservationNotFoundException;
import com.reserly.platform.reservations.service.VenueReservationService;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Verifica la frontera HTTP del panel sin arrancar infraestructura ni una base de datos.
 *
 * <p>La cadena focalizada reproduce la regla central de {@code SecurityConfiguration}: todo
 * {@code /api/venue/me/**} exige {@code ROLE_VENUE_OWNER}. Las pruebas de servicio y DAO verifican
 * por separado que ese propietario se aplica a cada consulta.
 */
@ExtendWith(MockitoExtension.class)
class VenueReservationPermissionTests {

  private static final String ENDPOINT = "/api/venue/me/reservations";

  @Mock private VenueReservationService reservationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller =
        new VenueReservationControllerImpl(
            reservationService, new VenueReservationConverter());
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new VenueReservationExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .apply(springSecurity(securityFilter()))
            .build();
  }

  @Test
  void rejectsAnonymousAndAdminBeforeInvokingReservationService() throws Exception {
    mockMvc
        .perform(get(ENDPOINT))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    mockMvc
        .perform(get(ENDPOINT).with(authentication(account("admin", "ROLE_ADMIN"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));

    verifyNoInteractions(reservationService);
  }

  @Test
  void authorizesVenueOwnerAndUsesOnlyPrincipalIdentityForList() throws Exception {
    AuthenticatedAccount principal = principal("venue_owner");
    LocalDate date = LocalDate.of(2026, 7, 24);
    when(reservationService.list(
            principal.userId(), "day", date, null, "confirmed", "ana", 0, 25))
        .thenReturn(Page.empty());

    mockMvc
        .perform(
            get(ENDPOINT)
                .with(authentication(account(principal, "ROLE_VENUE_OWNER")))
                .param("period", "day")
                .param("date", date.toString())
                .param("status", "confirmed")
                .param("user", "ana"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));

    verify(reservationService)
        .list(principal.userId(), "day", date, null, "confirmed", "ana", 0, 25);
  }

  @Test
  void authorizesVenueOwnerButKeepsForeignOrMissingDetailOpaque() throws Exception {
    AuthenticatedAccount principal = principal("venue_owner");
    UUID reservationId = UUID.randomUUID();
    when(reservationService.findDetail(principal.userId(), reservationId))
        .thenThrow(new VenueReservationNotFoundException());

    mockMvc
        .perform(
            get(ENDPOINT + "/" + reservationId)
                .with(authentication(account(principal, "ROLE_VENUE_OWNER"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("VENUE_RESERVATION_NOT_FOUND"));

    verify(reservationService).findDetail(principal.userId(), reservationId);
  }

  private FilterChainProxy securityFilter() {
    var authorization =
        RequestMatcherDelegatingAuthorizationManager.builder()
            .add(
                PathPatternRequestMatcher.pathPattern("/api/venue/me/**"),
                AuthorityAuthorizationManager.hasRole("VENUE_OWNER"))
            .build();
    var exceptionTranslation =
        new ExceptionTranslationFilter(new RestAuthenticationEntryPoint());
    exceptionTranslation.setAccessDeniedHandler(new RestAccessDeniedHandler());
    var chain =
        new DefaultSecurityFilterChain(
            AnyRequestMatcher.INSTANCE,
            new SecurityContextHolderFilter(new RequestAttributeSecurityContextRepository()),
            exceptionTranslation,
            new AuthorizationFilter(authorization));
    return new FilterChainProxy(chain);
  }

  private UsernamePasswordAuthenticationToken account(String role, String authority) {
    return account(principal(role), authority);
  }

  private UsernamePasswordAuthenticationToken account(
      AuthenticatedAccount principal, String authority) {
    return UsernamePasswordAuthenticationToken.authenticated(
        principal, null, Set.of(new SimpleGrantedAuthority(authority)));
  }

  private AuthenticatedAccount principal(String role) {
    return new AuthenticatedAccount(
        UUID.randomUUID(),
        UUID.randomUUID(),
        role.equals("admin") ? AccountType.ADMIN : AccountType.VENUE_BUSINESS,
        "es",
        Set.of(role));
  }
}
