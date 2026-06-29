package com.reserly.platform.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.identity.service.OneTimeTokenService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifica emisión, rotación y consumo sobre PostgreSQL real.
 *
 * <p>Los casos comprueban estados finales, caducidad, respuesta genérica e imposibilidad de
 * reutilizar un secreto.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmailVerificationIntegrationTests {

  private static final String VERIFY_ENDPOINT = "/api/auth/email/verify";
  private static final String REQUEST_ENDPOINT = "/api/auth/email/verification/request";

  @Autowired private WebApplicationContext applicationContext;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private OneTimeTokenService tokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
  }

  @Test
  void consumesValidTokenActivatesPendingAccountAndRejectsReuse() throws Exception {
    UUID userId = insertUser("pending@example.com", "pending_email_verification", null);
    String rawToken =
        insertToken(userId, Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post(VERIFY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyRequest(rawToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(jsonPath("$.emailVerifiedAt").isNotEmpty())
        .andExpect(jsonPath("$.accountStatus").value("active"))
        .andExpect(jsonPath("$.token").doesNotExist());

    Map<String, Object> user =
        jdbcTemplate.queryForMap(
            """
            SELECT "emailVerifiedAt", "status"
            FROM "Users"
            WHERE "id" = ?
            """,
            userId);
    assertThat(user.get("emailVerifiedAt")).isNotNull();
    assertThat(user.get("status")).isEqualTo("active");
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT "consumedAt" IS NOT NULL
                FROM "AuthTokens"
                WHERE "tokenHash" = ?
                """,
                Boolean.class,
                tokenService.hash(rawToken)))
        .isTrue();

    mockMvc
        .perform(
            post(VERIFY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyRequest(rawToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_INVALID"));
  }

  @Test
  void rejectsExpiredAndMalformedTokensWithoutVerifyingAccount() throws Exception {
    UUID userId = insertUser("expired@example.com", "pending_email_verification", null);
    String expiredToken =
        insertToken(userId, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));

    mockMvc
        .perform(
            post(VERIFY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyRequest(expiredToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_INVALID"));

    mockMvc
        .perform(
            post(VERIFY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_INVALID"));

    assertThat(emailVerifiedAt(userId)).isNull();
  }

  @Test
  void verifiesEmailWithoutReactivatingSuspendedAccount() throws Exception {
    UUID userId = insertUser("suspended@example.com", "suspended", null);
    String rawToken =
        insertToken(userId, Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post(VERIFY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyRequest(rawToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountStatus").value("suspended"));

    assertThat(emailVerifiedAt(userId)).isNotNull();
    assertThat(statusOf(userId)).isEqualTo("suspended");
  }

  @Test
  void rotatesPendingChallengeAndKeepsRequestResponseGeneric() throws Exception {
    UUID userId = insertUser("rotate@example.com", "pending_email_verification", null);
    String previousToken =
        insertToken(userId, Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post(REQUEST_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ROTATE@example.com\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$").doesNotExist());

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

    mockMvc
        .perform(
            post(REQUEST_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}"))
        .andExpect(status().isAccepted());
    assertThat(countAllTokens()).isEqualTo(2);
  }

  @Test
  void doesNotIssueAnotherChallengeForAlreadyVerifiedOrDisabledAccount() throws Exception {
    UUID verifiedUser =
        insertUser("verified@example.com", "active", Instant.now().minusSeconds(60));
    UUID disabledUser = insertUser("disabled@example.com", "disabled", null);

    requestChallenge("verified@example.com");
    requestChallenge("disabled@example.com");

    assertThat(countTokens(verifiedUser)).isZero();
    assertThat(countTokens(disabledUser)).isZero();
  }

  private void requestChallenge(String email) throws Exception {
    mockMvc
        .perform(
            post(REQUEST_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)))
        .andExpect(status().isAccepted());
  }

  private UUID insertUser(String email, String status, Instant verifiedAt) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbcTemplate.update(
        """
        INSERT INTO "Users" (
          "id",
          "email",
          "emailNormalized",
          "passwordHash",
          "accountType",
          "preferredLocale",
          "emailVerifiedAt",
          "status",
          "createdAt",
          "updatedAt"
        )
        VALUES (?, ?, ?, ?, 'venue_business', 'es', ?, ?, ?, ?)
        """,
        id,
        email,
        email,
        "$2b$12$abcdefghijklmnopqrstuuC6U4Jf90HIXRk2SPOlGzi4S2Vj4M7jO",
        timestamp(verifiedAt),
        status,
        Timestamp.from(now),
        Timestamp.from(now));
    return id;
  }

  private String insertToken(UUID userId, Instant createdAt, Instant expiresAt) {
    String rawToken = tokenService.generate();
    jdbcTemplate.update(
        """
        INSERT INTO "AuthTokens" (
          "id", "userId", "purpose", "tokenHash", "createdAt", "expiresAt"
        )
        VALUES (?, ?, 'email_verification', ?, ?, ?)
        """,
        UUID.randomUUID(),
        userId,
        tokenService.hash(rawToken),
        Timestamp.from(createdAt),
        Timestamp.from(expiresAt));
    return rawToken;
  }

  private Instant emailVerifiedAt(UUID userId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT "emailVerifiedAt"
        FROM "Users"
        WHERE "id" = ?
        """,
        Instant.class,
        userId);
  }

  private String statusOf(UUID userId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT "status"
        FROM "Users"
        WHERE "id" = ?
        """,
        String.class,
        userId);
  }

  private int countTokens(UUID userId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT count(*)
        FROM "AuthTokens"
        WHERE "userId" = ?
        """,
        Integer.class,
        userId);
  }

  private int countAllTokens() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM \"AuthTokens\"", Integer.class);
  }

  private String verifyRequest(String token) {
    return "{\"token\":\"%s\"}".formatted(token);
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }
}
