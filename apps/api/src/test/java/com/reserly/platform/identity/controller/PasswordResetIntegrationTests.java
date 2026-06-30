package com.reserly.platform.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.identity.service.OneTimeTokenService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Verifica recuperación, consumo y revocación de sesiones sobre PostgreSQL real. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTests {

  private static final String FORGOT_ENDPOINT = "/api/auth/password/forgot";
  private static final String RESET_ENDPOINT = "/api/auth/password/reset";
  private static final String OLD_PASSWORD = "old-password-value";
  private static final String NEW_PASSWORD = "new-password-value";

  @Autowired private WebApplicationContext applicationContext;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private OneTimeTokenService tokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
  }

  @Test
  void rotatesChallengeAndKeepsForgotResponseGeneric() throws Exception {
    UUID userId = insertVenueUser("reset@example.com", "active");
    String previousToken =
        insertToken(
            userId,
            "password_reset",
            Instant.now().minusSeconds(20),
            Instant.now().plusSeconds(900));

    forgot("RESET@example.com");

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT "revokedAt" IS NOT NULL
                FROM "AuthTokens"
                WHERE "tokenHash" = ?
                """,
                Boolean.class,
                tokenService.hash(previousToken)))
        .isTrue();
    assertThat(countTokens(userId)).isEqualTo(2);

    forgot("unknown@example.com");
    assertThat(countAllTokens()).isEqualTo(2);
  }

  @Test
  void resetsPasswordConsumesTokenRevokesSiblingsAndAllSessions() throws Exception {
    UUID userId = insertVenueUser("owner@example.com", "active");
    String rawToken =
        insertToken(
            userId,
            "password_reset",
            Instant.now().minusSeconds(20),
            Instant.now().plusSeconds(900));
    String siblingToken =
        insertToken(
            userId,
            "password_reset",
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(900));
    insertSession(userId, false);
    insertSession(userId, false);

    mockMvc
        .perform(
            post(RESET_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetRequest(rawToken, NEW_PASSWORD)))
        .andExpect(status().isNoContent());

    String passwordHash =
        jdbcTemplate.queryForObject(
            "SELECT \"passwordHash\" FROM \"Users\" WHERE \"id\" = ?", String.class, userId);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    assertThat(encoder.matches(NEW_PASSWORD, passwordHash)).isTrue();
    assertThat(encoder.matches(OLD_PASSWORD, passwordHash)).isFalse();
    assertThat(tokenFinalState(rawToken, "consumedAt")).isTrue();
    assertThat(tokenFinalState(siblingToken, "revokedAt")).isTrue();
    assertThat(activeSessionCount(userId)).isZero();

    mockMvc
        .perform(
            post(RESET_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetRequest(rawToken, "another-password-value")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("PASSWORD_RESET_INVALID"));
  }

  @Test
  void rejectsExpiredWrongPurposeMalformedAndOversizedPasswordUniformly() throws Exception {
    UUID userId = insertVenueUser("invalid@example.com", "active");
    String expiredToken =
        insertToken(
            userId,
            "password_reset",
            Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(3600));
    String emailToken =
        insertToken(
            userId,
            "email_verification",
            Instant.now().minusSeconds(20),
            Instant.now().plusSeconds(900));

    invalidReset(resetRequest(expiredToken, NEW_PASSWORD));
    invalidReset(resetRequest(emailToken, NEW_PASSWORD));
    invalidReset(resetRequest("short", NEW_PASSWORD));
    invalidReset(resetRequest(tokenService.generate(), "á".repeat(40)));

    assertThat(currentPasswordMatches(userId, OLD_PASSWORD)).isTrue();
  }

  @Test
  void permitsSuspendedAccountWithoutChangingStatusAndBlocksDisabledAccount() throws Exception {
    UUID suspendedId = insertVenueUser("suspended@example.com", "suspended");
    String suspendedToken =
        insertToken(
            suspendedId,
            "password_reset",
            Instant.now().minusSeconds(20),
            Instant.now().plusSeconds(900));
    UUID disabledId = insertVenueUser("disabled@example.com", "disabled");
    String disabledToken =
        insertToken(
            disabledId,
            "password_reset",
            Instant.now().minusSeconds(20),
            Instant.now().plusSeconds(900));

    mockMvc
        .perform(
            post(RESET_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetRequest(suspendedToken, NEW_PASSWORD)))
        .andExpect(status().isNoContent());
    assertThat(statusOf(suspendedId)).isEqualTo("suspended");

    invalidReset(resetRequest(disabledToken, NEW_PASSWORD));
    forgot("disabled@example.com");
    assertThat(countTokens(disabledId)).isEqualTo(1);
  }

  @Test
  void rejectsWeakOrMalformedPayloadWithStableError() throws Exception {
    invalidReset("{\"token\":\"short\",\"newPassword\":\"tiny\"}");

    mockMvc
        .perform(
            post(FORGOT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("PASSWORD_RESET_INVALID"));
  }

  private void forgot(String email) throws Exception {
    mockMvc
        .perform(
            post(FORGOT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$").doesNotExist());
  }

  private void invalidReset(String body) throws Exception {
    mockMvc
        .perform(post(RESET_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("PASSWORD_RESET_INVALID"));
  }

  private UUID insertVenueUser(String email, String status) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbcTemplate.update(
        """
        INSERT INTO "Users" (
          "id", "email", "emailNormalized", "passwordHash", "accountType",
          "preferredLocale", "emailVerifiedAt", "status", "createdAt", "updatedAt"
        )
        VALUES (?, ?, ?, ?, 'venue_business', 'es', ?, ?, ?, ?)
        """,
        id,
        email,
        email,
        new BCryptPasswordEncoder(4).encode(OLD_PASSWORD),
        Timestamp.from(now.minusSeconds(3600)),
        status,
        Timestamp.from(now),
        Timestamp.from(now));
    return id;
  }

  private String insertToken(UUID userId, String purpose, Instant createdAt, Instant expiresAt) {
    String rawToken = tokenService.generate();
    jdbcTemplate.update(
        """
        INSERT INTO "AuthTokens" (
          "id", "userId", "purpose", "tokenHash", "createdAt", "expiresAt"
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        userId,
        purpose,
        tokenService.hash(rawToken),
        Timestamp.from(createdAt),
        Timestamp.from(expiresAt));
    return rawToken;
  }

  private void insertSession(UUID userId, boolean revoked) {
    Instant now = Instant.now();
    jdbcTemplate.update(
        """
        INSERT INTO "AuthSessions" (
          "id", "userId", "tokenHash", "createdAt", "lastSeenAt", "expiresAt", "revokedAt"
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        userId,
        tokenService.hash(tokenService.generate()),
        Timestamp.from(now.minusSeconds(60)),
        Timestamp.from(now.minusSeconds(30)),
        Timestamp.from(now.plusSeconds(3600)),
        revoked ? Timestamp.from(now.minusSeconds(10)) : null);
  }

  private boolean tokenFinalState(String token, String column) {
    if (!"consumedAt".equals(column) && !"revokedAt".equals(column)) {
      throw new IllegalArgumentException("Unsupported token state column");
    }
    Boolean result =
        jdbcTemplate.queryForObject(
            "SELECT \"" + column + "\" IS NOT NULL FROM \"AuthTokens\" WHERE \"tokenHash\" = ?",
            Boolean.class,
            tokenService.hash(token));
    return Boolean.TRUE.equals(result);
  }

  private boolean currentPasswordMatches(UUID userId, String password) {
    String hash =
        jdbcTemplate.queryForObject(
            "SELECT \"passwordHash\" FROM \"Users\" WHERE \"id\" = ?", String.class, userId);
    return new BCryptPasswordEncoder().matches(password, hash);
  }

  private String statusOf(UUID userId) {
    return jdbcTemplate.queryForObject(
        "SELECT \"status\" FROM \"Users\" WHERE \"id\" = ?", String.class, userId);
  }

  private int activeSessionCount(UUID userId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT count(*)
        FROM "AuthSessions"
        WHERE "userId" = ? AND "revokedAt" IS NULL
        """,
        Integer.class,
        userId);
  }

  private int countTokens(UUID userId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM \"AuthTokens\" WHERE \"userId\" = ?", Integer.class, userId);
  }

  private int countAllTokens() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM \"AuthTokens\"", Integer.class);
  }

  private String resetRequest(String token, String password) {
    return "{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, password);
  }
}
