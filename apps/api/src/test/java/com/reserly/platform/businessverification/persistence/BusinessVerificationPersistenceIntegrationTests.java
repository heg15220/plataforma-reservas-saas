package com.reserly.platform.businessverification.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el esquema empresarial sobre PostgreSQL real.
 *
 * <p>Las pruebas cubren unicidad fiscal, minimización de respuestas, coherencia de revisión,
 * relaciones y protección frente a borrados que dejarían objetos privados huérfanos.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BusinessVerificationPersistenceIntegrationTests {

  private static final String PASSWORD_HASH = "$2a$12$placeholder.hash.for.persistence.contract";
  private static final String VALID_FILE_HASH =
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String VALID_RESPONSE_HASH =
      "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private BusinessAccountDao businessAccountDao;

  @Autowired private BusinessVerificationCheckDao businessVerificationCheckDao;

  @Autowired private BusinessVerificationDocumentDao businessVerificationDocumentDao;

  @Test
  void exposesRepositoriesAndExpectedUpperCamelCaseTables() {
    assertThat(businessAccountDao).isNotNull();
    assertThat(businessVerificationCheckDao).isNotNull();
    assertThat(businessVerificationDocumentDao).isNotNull();

    List<String> tables =
        jdbcTemplate.queryForList(
            """
            SELECT "table_name"
            FROM "information_schema"."tables"
            WHERE "table_schema" = 'public'
              AND "table_name" IN (
                'BusinessAccounts',
                'BusinessVerificationChecks',
                'BusinessVerificationDocuments'
              )
            ORDER BY "table_name"
            """,
            String.class);

    assertThat(tables)
        .containsExactly(
            "BusinessAccounts", "BusinessVerificationChecks", "BusinessVerificationDocuments");
  }

  @Test
  void createsBusinessAccountWithSafeInitialState() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");
    UUID accountId = insertBusinessAccount(ownerId, "ES", "B12345674", "B12345674");

    String status =
        jdbcTemplate.queryForObject(
            """
            SELECT "businessVerificationStatus"
            FROM "BusinessAccounts"
            WHERE "id" = ?
            """,
            String.class,
            accountId);

    assertThat(status).isEqualTo("unverified");
    assertThat(businessAccountDao.findById(accountId)).isPresent();
  }

  @Test
  void enforcesUniqueNormalizedIdentifierPerCountry() {
    UUID firstOwnerId = insertUser("first@example.com", "venue_business");
    UUID secondOwnerId = insertUser("second@example.com", "venue_business");
    insertBusinessAccount(firstOwnerId, "ES", "B-12345674", "B12345674");

    assertThatThrownBy(() -> insertBusinessAccount(secondOwnerId, "ES", "B 12345674", "B12345674"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uqBusinessAccountsTaxIdentifier");
  }

  @Test
  void rejectsLowercaseTaxCountry() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "BusinessAccounts" (
                      "ownerUserId",
                      "taxCountry",
                      "businessLegalName",
                      "businessTaxIdentifier",
                      "businessTaxIdentifierNormalized"
                    )
                    VALUES (?, 'es', 'Empresa SL', 'B12345674', 'B12345674')
                    """,
                    ownerId))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBusinessAccountsTaxCountry");
  }

  @Test
  void rejectsVerifiedStateWithoutVerificationTimestamp() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "BusinessAccounts" (
                      "ownerUserId",
                      "taxCountry",
                      "businessLegalName",
                      "businessTaxIdentifier",
                      "businessTaxIdentifierNormalized",
                      "businessVerificationStatus"
                    )
                    VALUES (?, 'ES', 'Empresa SL', 'B12345674', 'B12345674', 'verified')
                    """,
                    ownerId))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBusinessAccountsVerifiedAt");
  }

  @Test
  void storesOnlyMinimalRemoteEvidenceAndRejectsMalformedHash() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");
    UUID accountId = insertBusinessAccount(ownerId, "ES", "B12345674", "B12345674");

    List<String> forbiddenColumns =
        jdbcTemplate.queryForList(
            """
            SELECT "column_name"
            FROM "information_schema"."columns"
            WHERE "table_schema" = 'public'
              AND "table_name" = 'BusinessVerificationChecks'
              AND "column_name" IN ('rawResponse', 'rawResponseJson', 'responseBody')
            """,
            String.class);
    assertThat(forbiddenColumns).isEmpty();

    assertThatThrownBy(
            () -> insertCheck(accountId, "vies", "verified", "raw-provider-response", null, null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBusinessVerificationChecksRawHash");
  }

  @Test
  void requiresControlledErrorMetadataForFailedRemoteCheck() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");
    UUID accountId = insertBusinessAccount(ownerId, "ES", "B12345674", "B12345674");

    assertThatThrownBy(
            () -> insertCheck(accountId, "vies", "error", VALID_RESPONSE_HASH, null, null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBusinessVerificationChecksError");
  }

  @Test
  void enforcesOneAuditRecordPerRemoteRequest() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");
    UUID accountId = insertBusinessAccount(ownerId, "ES", "B12345674", "B12345674");
    UUID requestId = UUID.randomUUID();
    insertCheck(accountId, requestId, "vies", "verified", VALID_RESPONSE_HASH, "REFERENCE-1", null);

    assertThatThrownBy(
            () ->
                insertCheck(
                    accountId,
                    requestId,
                    "vies",
                    "verified",
                    VALID_RESPONSE_HASH,
                    "REFERENCE-2",
                    null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uqBusinessVerificationChecksRequestId");
  }

  @Test
  void requiresReviewerAndTimestampForFinalDocumentState() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");
    UUID reviewerId = insertUser("admin@example.com", "admin");
    UUID accountId = insertBusinessAccount(ownerId, "ES", "B12345674", "B12345674");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "BusinessVerificationDocuments" (
                      "businessAccountId",
                      "documentType",
                      "fileUrl",
                      "fileHash",
                      "status",
                      "uploadedByUserId",
                      "reviewedByUserId"
                    )
                    VALUES (?, 'census_certificate', ?, ?, 'accepted', ?, ?)
                    """,
                    accountId,
                    "private/business/account/document.pdf",
                    VALID_FILE_HASH,
                    ownerId,
                    reviewerId))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBusinessVerificationDocumentsReviewEvidence");
  }

  @Test
  void rejectsPersistentPublicUrlForSensitiveDocument() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");
    UUID accountId = insertBusinessAccount(ownerId, "ES", "B12345674", "B12345674");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "BusinessVerificationDocuments" (
                      "businessAccountId",
                      "documentType",
                      "fileUrl",
                      "fileHash",
                      "uploadedByUserId"
                    )
                    VALUES (?, 'census_certificate', ?, ?, ?)
                    """,
                    accountId,
                    "https://public.example/document.pdf",
                    VALID_FILE_HASH,
                    ownerId))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBusinessVerificationDocumentsPrivateLocator");
  }

  @Test
  void preventsDeletingAccountWhileAuditEvidenceExists() {
    UUID ownerId = insertUser("owner@example.com", "venue_business");
    UUID accountId = insertBusinessAccount(ownerId, "ES", "B12345674", "B12345674");
    insertCheck(accountId, "vies", "verified", VALID_RESPONSE_HASH, "VIES-REFERENCE-1", null);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    DELETE FROM "BusinessAccounts"
                    WHERE "id" = ?
                    """,
                    accountId))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fkBusinessVerificationChecksAccount");
  }

  private UUID insertUser(String email, String accountType) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "Users" ("email", "emailNormalized", "passwordHash", "accountType")
        VALUES (?, ?, ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        email,
        email.toLowerCase(),
        PASSWORD_HASH,
        accountType);
  }

  private UUID insertBusinessAccount(
      UUID ownerId, String taxCountry, String taxIdentifier, String normalizedIdentifier) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "BusinessAccounts" (
          "ownerUserId",
          "taxCountry",
          "businessLegalName",
          "businessTaxIdentifier",
          "businessTaxIdentifierNormalized"
        )
        VALUES (?, ?, 'Empresa de prueba SL', ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        ownerId,
        taxCountry,
        taxIdentifier,
        normalizedIdentifier);
  }

  private UUID insertCheck(
      UUID accountId,
      String provider,
      String status,
      String responseHash,
      String remoteReference,
      String errorCode) {
    return insertCheck(
        accountId, UUID.randomUUID(), provider, status, responseHash, remoteReference, errorCode);
  }

  private UUID insertCheck(
      UUID accountId,
      UUID requestId,
      String provider,
      String status,
      String responseHash,
      String remoteReference,
      String errorCode) {
    Instant checkedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    String errorMessageKey = errorCode == null ? null : "businessVerification.remoteError";

    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "BusinessVerificationChecks" (
          "businessAccountId",
          "requestId",
          "provider",
          "providerCountry",
          "identifierChecked",
          "status",
          "remoteReference",
          "checkedAt",
          "errorCode",
          "errorMessageKey",
          "rawResponseHash"
        )
        VALUES (?, ?, ?, 'ES', 'B12345674', ?, ?, ?, ?, ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        accountId,
        requestId,
        provider,
        status,
        remoteReference,
        Timestamp.from(checkedAt),
        errorCode,
        errorMessageKey,
        responseHash);
  }
}
