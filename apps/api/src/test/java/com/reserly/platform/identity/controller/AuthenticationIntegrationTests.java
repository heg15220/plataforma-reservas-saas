package com.reserly.platform.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.identity.service.PasswordHashingService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Verifica contratos HTTP, sesiones hasheadas, rehash y revocación sobre PostgreSQL real. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTests {

  private static final String LOGIN_ENDPOINT = "/api/auth/login";
  private static final String LOGOUT_ENDPOINT = "/api/auth/logout";
  private static final String PASSWORD = "correct-horse-battery-staple";

  @Autowired private WebApplicationContext applicationContext;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PasswordHashingService passwordHashingService;
  @Autowired private SessionTokenService sessionTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
  }

  @Test
  void logsInVerifiedVenueAndPersistsOnlySessionHash() throws Exception {
    UUID userId = insertUser("owner@example.com", "venue_business", "active", true, currentHash());

    MvcResult result =
        mockMvc
            .perform(login("OWNER@EXAMPLE.COM", PASSWORD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.accountType").value("venue_business"))
            .andExpect(jsonPath("$.preferredLocale").value("es"))
            .andExpect(jsonPath("$.emailVerified").value(true))
            .andExpect(jsonPath("$.sessionExpiresAt").isNotEmpty())
            .andExpect(jsonPath("$.sessionToken").doesNotExist())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("SameSite=Strict")))
            .andReturn();

    Cookie cookie = result.getResponse().getCookie(SessionCookieFactory.COOKIE_NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getValue()).matches("[A-Za-z0-9_-]{43}");
    Map<String, Object> session =
        jdbcTemplate.queryForMap(
            """
            SELECT "tokenHash", "createdAt", "lastSeenAt", "expiresAt", "revokedAt"
            FROM "AuthSessions"
            WHERE "userId" = ?
            """,
            userId);
    assertThat(session.get("tokenHash"))
        .isEqualTo(sessionTokenService.hash(cookie.getValue()))
        .isNotEqualTo(cookie.getValue());
    assertThat(session.get("createdAt")).isEqualTo(session.get("lastSeenAt"));
    assertThat(session.get("revokedAt")).isNull();
  }

  @Test
  void permitsPendingEmailAccountWithoutGrantingVerifiedState() throws Exception {
    insertUser(
        "pending@example.com",
        "venue_business",
        "pending_email_verification",
        false,
        currentHash());

    mockMvc
        .perform(login("pending@example.com", PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(false));
  }

  @Test
  void returnsSameGenericErrorForUnknownEmailAndWrongPassword() throws Exception {
    insertUser("owner@example.com", "venue_business", "active", true, currentHash());

    MvcResult unknown =
        mockMvc
            .perform(login("unknown@example.com", PASSWORD))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("AUTHENTICATION_INVALID"))
            .andReturn();
    MvcResult wrong =
        mockMvc
            .perform(login("owner@example.com", "wrong-password"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("AUTHENTICATION_INVALID"))
            .andReturn();

    assertThat(unknown.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(wrong.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(countSessions()).isZero();
  }

  @Test
  void rejectsSuspendedAndNonVenueAccountsAfterCredentialCheck() throws Exception {
    insertUser("suspended@example.com", "venue_business", "suspended", true, currentHash());
    insertUser("customer@example.com", "customer", "active", true, currentHash());

    mockMvc
        .perform(login("suspended@example.com", PASSWORD))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_INVALID"));
    mockMvc
        .perform(login("customer@example.com", PASSWORD))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_INVALID"));

    assertThat(countSessions()).isZero();
  }

  @Test
  void upgradesLegacyHashOnlyAfterSuccessfulAuthentication() throws Exception {
    String legacyHash = new BCryptPasswordEncoder(10).encode(PASSWORD);
    UUID userId = insertUser("legacy@example.com", "venue_business", "active", true, legacyHash);

    mockMvc.perform(login("legacy@example.com", PASSWORD)).andExpect(status().isOk());

    String upgraded =
        jdbcTemplate.queryForObject(
            """
            SELECT "passwordHash"
            FROM "Users"
            WHERE "id" = ?
            """,
            String.class,
            userId);
    assertThat(upgraded).startsWith("$2b$12$").isNotEqualTo(legacyHash);
    assertThat(passwordHashingService.matches(PASSWORD, upgraded)).isTrue();
  }

  @Test
  void logoutRevokesIdempotentlyAndAlwaysClearsCookie() throws Exception {
    UUID userId = insertUser("owner@example.com", "venue_business", "active", true, currentHash());
    Cookie sessionCookie =
        mockMvc
            .perform(login("owner@example.com", PASSWORD))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie(SessionCookieFactory.COOKIE_NAME);

    mockMvc
        .perform(post(LOGOUT_ENDPOINT).cookie(sessionCookie))
        .andExpect(status().isNoContent())
        .andExpect(
            header()
                .string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
    mockMvc.perform(post(LOGOUT_ENDPOINT).cookie(sessionCookie)).andExpect(status().isNoContent());
    mockMvc.perform(post(LOGOUT_ENDPOINT)).andExpect(status().isNoContent());

    Instant revokedAt =
        jdbcTemplate.queryForObject(
            """
            SELECT "revokedAt"
            FROM "AuthSessions"
            WHERE "userId" = ?
            """,
            Instant.class,
            userId);
    assertThat(revokedAt).isNotNull();
  }

  @Test
  void rejectsMalformedLoginWithoutCreatingSession() throws Exception {
    mockMvc
        .perform(
            post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_INVALID"));

    assertThat(countSessions()).isZero();
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
      String email, String password) {
    return post(LOGIN_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"email":"%s","password":"%s"}
            """
                .formatted(email, password));
  }

  private UUID insertUser(
      String email, String accountType, String status, boolean emailVerified, String passwordHash) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "Users" (
          "email",
          "emailNormalized",
          "passwordHash",
          "accountType",
          "preferredLocale",
          "emailVerifiedAt",
          "status"
        )
        VALUES (?, ?, ?, ?, 'es', ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        email,
        email,
        passwordHash,
        accountType,
        emailVerified ? Timestamp.from(Instant.now()) : null,
        status);
  }

  private String currentHash() {
    return passwordHashingService.hash(PASSWORD);
  }

  private int countSessions() {
    Integer count =
        jdbcTemplate.queryForObject("SELECT count(*) FROM \"AuthSessions\"", Integer.class);
    return count == null ? 0 : count;
  }
}
