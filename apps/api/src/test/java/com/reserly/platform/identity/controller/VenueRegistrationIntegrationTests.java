package com.reserly.platform.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.identity.service.OneTimeTokenService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifica el contrato HTTP y la transacción completa de registro sobre PostgreSQL real.
 *
 * <p>Las aserciones comprueban que el cliente no controla privilegios, el secreto queda hasheado,
 * el rol se asigna y cualquier conflicto revierte todas las escrituras.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VenueRegistrationIntegrationTests {

  private static final String ENDPOINT = "/api/auth/venues/register";
  private static final String VERIFY_EMAIL_ENDPOINT = "/api/auth/email/verify";
  private static final String LOGIN_ENDPOINT = "/api/auth/login";
  private static final String DOCUMENT_REQUEST_ENDPOINT =
      "/api/venue/me/business-verification/document-request";
  private static final String DOCUMENT_UPLOAD_ENDPOINT =
      "/api/venue/me/business-verification/documents";
  private static final String RAW_PASSWORD = "correct-horse-battery-staple";

  @Autowired private WebApplicationContext applicationContext;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private OneTimeTokenService tokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
  }

  @Test
  void registersVenueBusinessAccountAndOwnerRoleAtomically() throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("Local@Example.com", "ES/B-12345674")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.businessAccountId").isNotEmpty())
        .andExpect(jsonPath("$.accountType").value("venue_business"))
        .andExpect(jsonPath("$.businessVerificationStatus").value("unverified"))
        .andExpect(jsonPath("$.emailVerificationRequired").value(true))
        .andExpect(jsonPath("$.canPublishVenue").value(false))
        .andExpect(jsonPath("$.password").doesNotExist());

    Map<String, Object> user =
        jdbcTemplate.queryForMap(
            """
            SELECT "id", "emailNormalized", "passwordHash", "accountType", "status"
            FROM "Users"
            WHERE "emailNormalized" = 'local@example.com'
            """);

    assertThat(user.get("emailNormalized")).isEqualTo("local@example.com");
    assertThat(user.get("accountType")).isEqualTo("venue_business");
    assertThat(user.get("status")).isEqualTo("pending_email_verification");
    assertThat(user.get("passwordHash")).isNotEqualTo(RAW_PASSWORD);
    assertThat(new BCryptPasswordEncoder().matches(RAW_PASSWORD, (String) user.get("passwordHash")))
        .isTrue();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "AuthTokens"
                WHERE "userId" = ?
                  AND "purpose" = 'email_verification'
                  AND "consumedAt" IS NULL
                  AND "revokedAt" IS NULL
                """,
                Integer.class,
                user.get("id")))
        .isEqualTo(1);

    Map<String, Object> business =
        jdbcTemplate.queryForMap(
            """
            SELECT
              "taxCountry",
              "businessLegalName",
              "businessTaxIdentifier",
              "businessTaxIdentifierNormalized",
              "businessVerificationStatus"
            FROM "BusinessAccounts"
            WHERE "ownerUserId" = ?
            """,
            user.get("id"));

    assertThat(business)
        .containsEntry("taxCountry", "ES")
        .containsEntry("businessLegalName", "Empresa de Prueba SL")
        .containsEntry("businessTaxIdentifier", "ES/B-12345674")
        .containsEntry("businessTaxIdentifierNormalized", "B12345674")
        .containsEntry("businessVerificationStatus", "unverified");

    String role =
        jdbcTemplate.queryForObject(
            """
            SELECT role."code"
            FROM "UserRoles" assignment
            JOIN "Roles" role ON role."id" = assignment."roleId"
            WHERE assignment."userId" = ?
            """,
            String.class,
            user.get("id"));
    assertThat(role).isEqualTo("venue_owner");
  }

  @Test
  void returnsGenericConflictForDuplicateEmail() throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("local@example.com", "B12345674")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("LOCAL@example.com", "B87654323")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("REGISTRATION_CONFLICT"));

    assertThat(countUsers()).isEqualTo(1);
    assertThat(countBusinessAccounts()).isEqualTo(1);
  }

  @Test
  void rollsBackUserWhenBusinessIdentifierConflicts() throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("first@example.com", "B-12345674")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("second@example.com", "es b.1234567-4")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("REGISTRATION_CONFLICT"));

    assertThat(countUsers()).isEqualTo(1);
    assertThat(countBusinessAccounts()).isEqualTo(1);
    Integer secondUserCount =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM "Users"
            WHERE "emailNormalized" = 'second@example.com'
            """,
            Integer.class);
    assertThat(secondUserCount).isZero();
  }

  @Test
  void rejectsInvalidPayloadWithoutWritingPartialData() throws Exception {
    String invalidRequest =
        """
        {
          "account": {
            "email": "not-an-email",
            "password": "short",
            "preferredLocale": "fr"
          },
          "business": {
            "taxCountry": "Spain",
            "legalName": "",
            "taxIdentifier": ""
          },
          "acceptsLegalTerms": false
        }
        """;

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("REGISTRATION_INVALID"));

    assertThat(countUsers()).isZero();
    assertThat(countBusinessAccounts()).isZero();
  }

  @Test
  void rejectsPasswordAboveBcryptByteLimit() throws Exception {
    String multiBytePassword = "á".repeat(40);
    String request =
        validRequest("local@example.com", "B12345674").replace(RAW_PASSWORD, multiBytePassword);

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("REGISTRATION_INVALID"));

    assertThat(countUsers()).isZero();
  }

  @Test
  void rejectsInvalidSpanishControlCharacterWithoutWritingPartialData() throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("local@example.com", "B12345678")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("REGISTRATION_INVALID"));

    assertThat(countUsers()).isZero();
    assertThat(countBusinessAccounts()).isZero();
  }

  @Test
  void completesOwnerJourneyFromRegistrationToPrivateDocumentRequest() throws Exception {
    register("journey@example.com", "B12345674");
    UUID userId = userId("journey@example.com");
    String emailToken = insertEmailVerificationToken(userId);

    mockMvc
        .perform(
            post(VERIFY_EMAIL_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(emailToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(jsonPath("$.accountStatus").value("active"));

    Cookie sessionCookie = login("journey@example.com");
    UUID requestId = createOpenDocumentRequest("journey@example.com");

    mockMvc
        .perform(get(DOCUMENT_REQUEST_ENDPOINT).cookie(sessionCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value(requestId.toString()))
        .andExpect(jsonPath("$.reasonCode").value("no_automated_channel"))
        .andExpect(jsonPath("$.requestedDocumentTypes[0]").value("census_certificate"))
        .andExpect(jsonPath("$.status").value("open"))
        .andExpect(jsonPath("$.businessAccountId").doesNotExist())
        .andExpect(jsonPath("$.sourceVerificationCheckId").doesNotExist());

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM "AuthSessions"
                WHERE "userId" = ?
                  AND "revokedAt" IS NULL
                """,
                Integer.class,
                userId))
        .isEqualTo(1);
  }

  @Test
  void isolatesDocumentRequestsBetweenAuthenticatedVenueOwners() throws Exception {
    mockMvc
        .perform(get(DOCUMENT_REQUEST_ENDPOINT))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));

    register("first-owner@example.com", "B12345674");
    register("second-owner@example.com", "B87654323");
    Cookie firstOwnerSession = login("first-owner@example.com");
    Cookie secondOwnerSession = login("second-owner@example.com");
    UUID firstOwnerRequest = createOpenDocumentRequest("first-owner@example.com");

    mockMvc
        .perform(get(DOCUMENT_REQUEST_ENDPOINT).cookie(firstOwnerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value(firstOwnerRequest.toString()));
    mockMvc
        .perform(get(DOCUMENT_REQUEST_ENDPOINT).cookie(secondOwnerSession))
        .andExpect(status().isNoContent());

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "evidence.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4\nfixture".getBytes(StandardCharsets.US_ASCII));
    mockMvc
        .perform(
            multipart(DOCUMENT_UPLOAD_ENDPOINT)
                .file(file)
                .param("documentRequestId", firstOwnerRequest.toString())
                .param("documentType", "census_certificate")
                .cookie(secondOwnerSession))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("DOCUMENT_UPLOAD_FORBIDDEN"));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"BusinessVerificationDocuments\"", Integer.class))
        .isZero();
  }

  private void register(String email, String taxIdentifier) throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest(email, taxIdentifier)))
        .andExpect(status().isCreated());
  }

  private Cookie login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """
                            .formatted(email, RAW_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountType").value("venue_business"))
            .andReturn();
    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).isNotBlank().startsWith(SessionCookieFactory.COOKIE_NAME + "=");
    String token = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    return new Cookie(SessionCookieFactory.COOKIE_NAME, token);
  }

  private String insertEmailVerificationToken(UUID userId) {
    String rawToken = tokenService.generate();
    Instant now = Instant.now();
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
        Timestamp.from(now),
        Timestamp.from(now.plusSeconds(3_600)));
    return rawToken;
  }

  private UUID createOpenDocumentRequest(String ownerEmail) {
    UUID accountId =
        jdbcTemplate.queryForObject(
            """
            SELECT account."id"
            FROM "BusinessAccounts" account
            JOIN "Users" owner ON owner."id" = account."ownerUserId"
            WHERE owner."emailNormalized" = ?
            """,
            UUID.class,
            ownerEmail);
    jdbcTemplate.update(
        """
        UPDATE "BusinessAccounts"
        SET "businessVerificationStatus" = 'pending_review',
            "updatedAt" = CURRENT_TIMESTAMP
        WHERE "id" = ?
        """,
        accountId);
    UUID checkId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO "BusinessVerificationChecks" (
            "businessAccountId", "requestId", "provider", "providerCountry",
              "status", "checkedAt", "attemptCount", "durationMs"
            )
            VALUES (?, ?, 'aeat-census-manual', 'ES',
                    'inconclusive', CURRENT_TIMESTAMP, 1, 0)
            RETURNING "id"
            """,
            UUID.class,
            accountId,
            UUID.randomUUID());
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "BusinessVerificationDocumentRequests" (
          "businessAccountId", "sourceVerificationCheckId", "reasonCode",
          "requestedDocumentTypes", "status", "requestedAt", "createdAt", "updatedAt"
        )
        VALUES (?, ?, 'no_automated_channel', ARRAY['census_certificate']::varchar[],
                'open', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING "id"
        """,
        UUID.class,
        accountId,
        checkId);
  }

  private UUID userId(String email) {
    return jdbcTemplate.queryForObject(
        """
        SELECT "id"
        FROM "Users"
        WHERE "emailNormalized" = ?
        """,
        UUID.class,
        email);
  }

  private String validRequest(String email, String taxIdentifier) {
    return """
        {
          "account": {
            "email": "%s",
            "password": "%s",
            "preferredLocale": "es"
          },
          "business": {
            "taxCountry": "es",
            "legalName": "  Empresa de Prueba SL  ",
            "taxIdentifier": "%s",
            "registeredAddress": "  Calle Ejemplo 1  "
          },
          "acceptsLegalTerms": true
        }
        """
        .formatted(email, RAW_PASSWORD, taxIdentifier);
  }

  private int countUsers() {
    Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM \"Users\"", Integer.class);
    return count == null ? 0 : count;
  }

  private int countBusinessAccounts() {
    Integer count =
        jdbcTemplate.queryForObject("SELECT count(*) FROM \"BusinessAccounts\"", Integer.class);
    return count == null ? 0 : count;
  }
}
