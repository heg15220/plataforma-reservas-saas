package com.reserly.platform.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.identity.controller.SessionCookieFactory;
import com.reserly.platform.identity.service.SessionTokenService;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifica autenticación de cookie y autorización de namespaces sobre PostgreSQL real.
 *
 * <p>El controlador importado existe solo como sonda: las rutas funcionales privadas se
 * incorporarán en sus tareas y heredarán la misma cadena de seguridad.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(RoleAuthorizationIntegrationTests.SecurityProbeController.class)
class RoleAuthorizationIntegrationTests {

  private static final String VENUE_ENDPOINT = "/api/venue/me/security-probe";
  private static final String ADMIN_ENDPOINT = "/api/admin/security-probe";
  private static final String PUBLIC_ENDPOINT = "/api/public/security-probe";
  private static final String NEARBY_PUBLIC_ENDPOINT = "/api/venue/mechanical";
  private static final String PASSWORD_HASH = "$2a$12$placeholder.hash.for.security.tests";

  @Autowired private WebApplicationContext applicationContext;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private SessionTokenService sessionTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
  }

  @Test
  void leavesDeclaredPublicNamespaceAccessibleAndDeniesUnknownApiRoutes() throws Exception {
    mockMvc
        .perform(get(PUBLIC_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access").value("public"));
    mockMvc
        .perform(get(NEARBY_PUBLIC_ENDPOINT))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));
  }

  @Test
  void permitsPreflightOnlyForConfiguredCredentialedOrigin() throws Exception {
    mockMvc
        .perform(
            options(VENUE_ENDPOINT)
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

    mockMvc
        .perform(
            options(VENUE_ENDPOINT)
                .header(HttpHeaders.ORIGIN, "https://attacker.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }

  @Test
  void returnsSameUnauthorizedContractForMissingMalformedAndUnknownSession() throws Exception {
    assertAuthenticationRequired(null);
    assertAuthenticationRequired(new Cookie(SessionCookieFactory.COOKIE_NAME, "malformed"));
    assertAuthenticationRequired(sessionCookie(sessionTokenService.generate()));
  }

  @Test
  void rejectsExpiredRevokedAndDuplicateSessionCookiesUniformly() throws Exception {
    UUID userId = insertUser("venue_business", "active");
    assignRole(userId, "venue_owner");
    String expired =
        insertSession(userId, Instant.now().minusSeconds(600), Instant.now().minusSeconds(1), null);
    String revoked =
        insertSession(
            userId,
            Instant.now().minusSeconds(600),
            Instant.now().plusSeconds(600),
            Instant.now().minusSeconds(1));

    assertAuthenticationRequired(sessionCookie(expired));
    assertAuthenticationRequired(sessionCookie(revoked));
    mockMvc
        .perform(
            get(VENUE_ENDPOINT)
                .cookie(sessionCookie(expired), sessionCookie(sessionTokenService.generate())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
  }

  @Test
  void authenticatesVenueOwnerAndTouchesOldActivityWithoutExtendingExpiry() throws Exception {
    UUID userId = insertUser("venue_business", "active");
    assignRole(userId, "venue_owner");
    Instant previousActivity = Instant.now().minusSeconds(600);
    Instant expiresAt = Instant.now().plusSeconds(3_600);
    String token = insertSession(userId, previousActivity, expiresAt, null);
    Instant persistedExpiresAt =
        jdbcTemplate.queryForObject(
            """
            SELECT "expiresAt"
            FROM "AuthSessions"
            WHERE "tokenHash" = ?
            """,
            Instant.class,
            sessionTokenService.hash(token));

    mockMvc
        .perform(get(VENUE_ENDPOINT).cookie(sessionCookie(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.accountType").value("venue_business"))
        .andExpect(jsonPath("$.roles[0]").value("venue_owner"));

    Map<String, Object> session =
        jdbcTemplate.queryForMap(
            """
            SELECT "lastSeenAt", "expiresAt"
            FROM "AuthSessions"
            WHERE "tokenHash" = ?
            """,
            sessionTokenService.hash(token));
    assertThat(((Timestamp) session.get("lastSeenAt")).toInstant()).isAfter(previousActivity);
    assertThat(((Timestamp) session.get("expiresAt")).toInstant()).isEqualTo(persistedExpiresAt);
  }

  @Test
  void permitsPendingEmailVenueOwnerButDoesNotGrantAdminNamespace() throws Exception {
    UUID userId = insertUser("venue_business", "pending_email_verification");
    assignRole(userId, "venue_owner");
    Cookie cookie = activeSessionCookie(userId);

    mockMvc.perform(get(VENUE_ENDPOINT).cookie(cookie)).andExpect(status().isOk());
    mockMvc
        .perform(get(ADMIN_ENDPOINT).cookie(cookie))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));
  }

  @Test
  void grantsAdminOnlyFromExplicitPersistedRole() throws Exception {
    UUID adminId = insertUser("admin", "active");
    assignRole(adminId, "admin");
    Cookie adminCookie = activeSessionCookie(adminId);

    mockMvc
        .perform(get(ADMIN_ENDPOINT).cookie(adminCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(adminId.toString()))
        .andExpect(jsonPath("$.roles[0]").value("admin"));
    mockMvc
        .perform(get(VENUE_ENDPOINT).cookie(adminCookie))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));
  }

  @Test
  void authenticatesAccountWithoutRoleButDeniesPrivateNamespace() throws Exception {
    UUID userId = insertUser("venue_business", "active");

    mockMvc
        .perform(get(VENUE_ENDPOINT).cookie(activeSessionCookie(userId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("AUTHORIZATION_DENIED"));
  }

  @Test
  void revokesObservedSessionWhenAccountIsNoLongerOperational() throws Exception {
    UUID userId = insertUser("venue_business", "suspended");
    assignRole(userId, "venue_owner");
    Cookie cookie = activeSessionCookie(userId);

    assertAuthenticationRequired(cookie);

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT "revokedAt" IS NOT NULL
                FROM "AuthSessions"
                WHERE "tokenHash" = ?
                """,
                Boolean.class,
                sessionTokenService.hash(cookie.getValue())))
        .isTrue();
  }

  private void assertAuthenticationRequired(Cookie cookie) throws Exception {
    var request = get(VENUE_ENDPOINT);
    if (cookie != null) {
      request.cookie(cookie);
    }
    mockMvc
        .perform(request)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
  }

  private UUID insertUser(String accountType, String status) {
    UUID id = UUID.randomUUID();
    String email = id + "@example.com";
    jdbcTemplate.update(
        """
        INSERT INTO "Users" (
          "id", "email", "emailNormalized", "passwordHash", "accountType",
          "preferredLocale", "status", "createdAt", "updatedAt"
        )
        VALUES (?, ?, ?, ?, ?, 'es', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        email,
        email,
        PASSWORD_HASH,
        accountType,
        status);
    return id;
  }

  private void assignRole(UUID userId, String roleCode) {
    jdbcTemplate.update(
        """
        INSERT INTO "UserRoles" ("userId", "roleId", "assignedAt")
        SELECT ?, role."id", CURRENT_TIMESTAMP
        FROM "Roles" role
        WHERE role."code" = ?
        """,
        userId,
        roleCode);
  }

  private Cookie activeSessionCookie(UUID userId) {
    String token =
        insertSession(
            userId, Instant.now().minusSeconds(600), Instant.now().plusSeconds(3_600), null);
    return sessionCookie(token);
  }

  private String insertSession(
      UUID userId, Instant lastSeenAt, Instant expiresAt, Instant revokedAt) {
    String token = sessionTokenService.generate();
    jdbcTemplate.update(
        """
        INSERT INTO "AuthSessions" (
          "userId", "tokenHash", "createdAt", "lastSeenAt", "expiresAt", "revokedAt"
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        userId,
        sessionTokenService.hash(token),
        Timestamp.from(lastSeenAt.minusSeconds(60)),
        Timestamp.from(lastSeenAt),
        Timestamp.from(expiresAt),
        revokedAt == null ? null : Timestamp.from(revokedAt));
    return token;
  }

  private Cookie sessionCookie(String token) {
    return new Cookie(SessionCookieFactory.COOKIE_NAME, token);
  }

  @RestController
  static class SecurityProbeController {

    @GetMapping(VENUE_ENDPOINT)
    Map<String, Object> venue(@AuthenticationPrincipal AuthenticatedAccount principal) {
      return principalResponse(principal);
    }

    @GetMapping(ADMIN_ENDPOINT)
    Map<String, Object> admin(@AuthenticationPrincipal AuthenticatedAccount principal) {
      return principalResponse(principal);
    }

    @GetMapping(PUBLIC_ENDPOINT)
    Map<String, String> publicAccess() {
      return Map.of("access", "public");
    }

    @GetMapping(NEARBY_PUBLIC_ENDPOINT)
    Map<String, String> nearbyPublicAccess() {
      return Map.of("access", "public");
    }

    private Map<String, Object> principalResponse(AuthenticatedAccount principal) {
      return Map.of(
          "userId", principal.userId(),
          "accountType", principal.accountType().persistedValue(),
          "roles", principal.roles().stream().sorted().toList());
    }
  }
}
