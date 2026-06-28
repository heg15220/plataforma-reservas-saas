package com.reserly.platform.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
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
  private static final String RAW_PASSWORD = "correct-horse-battery-staple";

  @Autowired private WebApplicationContext applicationContext;

  @Autowired private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
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
