package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationAdapter;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationException;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationRequest;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationResult;
import com.reserly.platform.businessverification.remote.RemoteVerificationAttemptContext;
import com.reserly.platform.businessverification.remote.RemoteVerificationErrorCode;
import com.reserly.platform.businessverification.remote.RemoteVerificationStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica ejecución, reintento, auditoría e idempotencia sobre PostgreSQL real.
 *
 * <p>El adaptador de prueba es determinista y no representa un proveedor de producción.
 */
@SpringBootTest(
    properties = {
      "reserly.business-verification.remote.initial-backoff=0ms",
      "reserly.business-verification.remote.max-backoff=0ms"
    })
@ActiveProfiles("test")
@Import(RemoteBusinessVerificationServiceIntegrationTests.AdapterConfiguration.class)
class RemoteBusinessVerificationServiceIntegrationTests {

  private static final String PASSWORD_HASH = "$2a$12$placeholder.hash.for.persistence.contract";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private RemoteBusinessVerificationService verificationService;

  @Autowired private DeterministicRemoteAdapter adapter;

  @Autowired private BusinessVerificationStateService verificationStateService;

  @Autowired private BusinessVerificationDocumentRequestService documentRequestService;

  private final List<UUID> createdAccountIds = new ArrayList<>();
  private final List<UUID> createdOwnerIds = new ArrayList<>();

  @BeforeEach
  void resetAdapter() {
    adapter.reset();
  }

  @AfterEach
  void removeCommittedFixtures() {
    for (UUID accountId : createdAccountIds) {
      jdbcTemplate.update(
          """
          DELETE FROM "BusinessVerificationDocumentRequests"
          WHERE "businessAccountId" = ?
          """,
          accountId);
      jdbcTemplate.update(
          """
          DELETE FROM "BusinessVerificationChecks"
          WHERE "businessAccountId" = ?
          """,
          accountId);
      jdbcTemplate.update(
          """
          DELETE FROM "BusinessAccounts"
          WHERE "id" = ?
          """,
          accountId);
    }
    for (UUID ownerId : createdOwnerIds) {
      jdbcTemplate.update(
          """
          DELETE FROM "Users"
          WHERE "id" = ?
          """,
          ownerId);
    }
    createdAccountIds.clear();
    createdOwnerIds.clear();
  }

  @Test
  void retriesPersistsMinimalEvidenceAndReusesSameRequest() {
    UUID accountId = insertBusinessAccount("ZZ");
    UUID requestId = UUID.randomUUID();
    adapter.failuresBeforeSuccess.set(1);

    RemoteBusinessVerificationOutcome first =
        verificationService.verify(
            new RemoteBusinessVerificationCommand(requestId, accountId, null));
    RemoteBusinessVerificationOutcome repeated =
        verificationService.verify(
            new RemoteBusinessVerificationCommand(requestId, accountId, null));

    assertThat(first.technicalStatus()).isEqualTo("verified");
    assertThat(first.providerCode()).isEqualTo("official-test");
    assertThat(first.attemptCount()).isEqualTo((short) 2);
    assertThat(first.businessVerificationStatus()).isEqualTo("verified");
    assertThat(first.businessVerificationExpiresAt())
        .isEqualTo(Instant.parse("2027-06-28T12:00:00Z"));
    assertThat(repeated.verificationCheckId()).isEqualTo(first.verificationCheckId());
    assertThat(adapter.invocations).hasValue(2);
    assertThat(documentRequestService.findOpen(accountId)).isEmpty();

    Map<String, Object> evidence =
        jdbcTemplate.queryForMap(
            """
            SELECT
              "requestId",
              "provider",
              "providerCountry",
              "identifierChecked",
              "status",
              "remoteReference",
              "attemptCount",
              "durationMs",
              "errorCode",
              "errorMessageKey"
            FROM "BusinessVerificationChecks"
            WHERE "id" = ?
            """,
            first.verificationCheckId());

    assertThat(evidence)
        .containsEntry("requestId", requestId)
        .containsEntry("provider", "official-test")
        .containsEntry("providerCountry", "ZZ")
        .containsEntry("status", "verified")
        .containsEntry("remoteReference", "TEST-" + requestId)
        .containsEntry("errorCode", null)
        .containsEntry("errorMessageKey", null);
    assertThat(((Number) evidence.get("attemptCount")).intValue()).isEqualTo(2);
    assertThat((Integer) evidence.get("durationMs")).isGreaterThanOrEqualTo(0);
    assertThat((String) evidence.get("identifierChecked")).startsWith("TEST");
  }

  @Test
  void persistsControlledErrorWhenCountryHasNoAdapter() {
    UUID accountId = insertBusinessAccount("YY");
    UUID requestId = UUID.randomUUID();

    RemoteBusinessVerificationOutcome outcome =
        verificationService.verify(
            new RemoteBusinessVerificationCommand(requestId, accountId, null));

    assertThat(outcome.technicalStatus()).isEqualTo("error");
    assertThat(outcome.providerCode()).isEqualTo("unavailable");
    assertThat(outcome.attemptCount()).isZero();
    assertThat(outcome.businessVerificationStatus()).isEqualTo("pending_review");
    assertThat(adapter.invocations).hasValue(0);

    Map<String, Object> evidence =
        jdbcTemplate.queryForMap(
            """
            SELECT "errorCode", "errorMessageKey", "attemptCount"
            FROM "BusinessVerificationChecks"
            WHERE "id" = ?
            """,
            outcome.verificationCheckId());
    assertThat(evidence)
        .containsEntry("errorCode", "NO_ADAPTER_CONFIGURED")
        .containsEntry("errorMessageKey", "businessVerification.remote.noAdapter");
    assertThat(((Number) evidence.get("attemptCount")).intValue()).isZero();

    BusinessVerificationDocumentRequestSnapshot request =
        documentRequestService.findOpen(accountId).orElseThrow();
    assertThat(request.reasonCode()).isEqualTo("provider_unavailable");
    assertThat(request.requestedDocumentTypes())
        .containsExactly("equivalent_administrative_document", "other");
  }

  @Test
  void rejectsReusingRequestIdForAnotherBusinessAccount() {
    UUID firstAccountId = insertBusinessAccount("ZZ");
    UUID secondAccountId = insertBusinessAccount("ZY");
    UUID requestId = UUID.randomUUID();

    verificationService.verify(
        new RemoteBusinessVerificationCommand(requestId, firstAccountId, null));

    assertThatThrownBy(
            () ->
                verificationService.verify(
                    new RemoteBusinessVerificationCommand(requestId, secondAccountId, null)))
        .isInstanceOf(RemoteVerificationRequestConflictException.class)
        .hasMessageNotContaining(requestId.toString());
    assertThat(adapter.invocations).hasValue(1);
  }

  @Test
  void routesSpanishNationalNifToAeatManualReviewWithoutNetwork() {
    UUID accountId = insertBusinessAccount("ES", "B-12345674", "B12345674");

    RemoteBusinessVerificationOutcome outcome =
        verificationService.verify(
            new RemoteBusinessVerificationCommand(UUID.randomUUID(), accountId, null));

    assertThat(outcome.providerCode()).isEqualTo("aeat-census-manual");
    assertThat(outcome.technicalStatus()).isEqualTo("inconclusive");
    assertThat(outcome.attemptCount()).isEqualTo((short) 1);
    assertThat(outcome.businessVerificationStatus()).isEqualTo("pending_review");
    assertThat(adapter.invocations).hasValue(0);

    BusinessVerificationDocumentRequestSnapshot request =
        documentRequestService.findOpen(accountId).orElseThrow();
    assertThat(request.reasonCode()).isEqualTo("no_automated_channel");
    assertThat(request.reasonMessageKey())
        .isEqualTo("businessVerification.documents.reason.no_automated_channel");
    assertThat(request.requestedDocumentTypes())
        .containsExactly(
            "census_registration_036_037",
            "census_certificate",
            "activity_or_opening_license",
            "equivalent_administrative_document",
            "other");
  }

  @Test
  void rejectsOfficiallyInvalidIdentifier() {
    UUID accountId = insertBusinessAccount("ZZ");
    adapter.resultStatus = RemoteVerificationStatus.INVALID;

    RemoteBusinessVerificationOutcome outcome =
        verificationService.verify(
            new RemoteBusinessVerificationCommand(UUID.randomUUID(), accountId, null));

    assertThat(outcome.technicalStatus()).isEqualTo("invalid");
    assertThat(outcome.businessVerificationStatus()).isEqualTo("rejected");
    assertThat(outcome.businessVerificationExpiresAt()).isNull();
    assertThat(documentRequestService.findOpen(accountId)).isEmpty();
  }

  @Test
  void sendsVerifiedNameMismatchToManualReview() {
    UUID accountId = insertBusinessAccount("ZZ");
    adapter.matchedLegalName = false;

    RemoteBusinessVerificationOutcome outcome =
        verificationService.verify(
            new RemoteBusinessVerificationCommand(UUID.randomUUID(), accountId, null));

    assertThat(outcome.technicalStatus()).isEqualTo("verified");
    assertThat(outcome.businessVerificationStatus()).isEqualTo("pending_review");
    assertThat(documentRequestService.findOpen(accountId).orElseThrow().reasonCode())
        .isEqualTo("legal_name_unconfirmed");
  }

  @Test
  void expiresDueVerifiedAccounts() {
    UUID accountId = insertBusinessAccount("ZZ");
    verificationService.verify(
        new RemoteBusinessVerificationCommand(UUID.randomUUID(), accountId, null));

    int expired =
        verificationStateService.expireDueVerifications(Instant.parse("2027-06-29T00:00:00Z"));

    assertThat(expired).isGreaterThanOrEqualTo(1);
    assertThat(verificationStateService.current(accountId).status())
        .isEqualTo(BusinessVerificationStatus.EXPIRED);
  }

  @Test
  void exposesPendingRemoteStateAndPreventsOverlappingChecks() {
    UUID accountId = insertBusinessAccount("ZZ");
    UUID requestId = UUID.randomUUID();

    BusinessVerificationStateSnapshot pending =
        verificationStateService.beginRemoteCheck(accountId, requestId);

    assertThat(pending.status()).isEqualTo(BusinessVerificationStatus.PENDING_REMOTE_CHECK);
    assertThatThrownBy(
            () -> verificationStateService.beginRemoteCheck(accountId, UUID.randomUUID()))
        .isInstanceOf(BusinessVerificationInProgressException.class)
        .hasMessageNotContaining(accountId.toString());
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT "activeVerificationRequestId"
                FROM "BusinessAccounts"
                WHERE "id" = ?
                """,
                UUID.class,
                accountId))
        .isEqualTo(requestId);
  }

  @Test
  void revalidationClearsPreviousManualDecisionEvidence() {
    UUID accountId = insertBusinessAccount("ZZ");
    UUID reviewerId = insertTrackedUser("admin");
    jdbcTemplate.update(
        """
        UPDATE "BusinessAccounts"
        SET "manualReviewStatus" = 'approved',
            "manualReviewedByUserId" = ?,
            "manualReviewedAt" = CURRENT_TIMESTAMP
        WHERE "id" = ?
        """,
        reviewerId,
        accountId);

    verificationStateService.beginRemoteCheck(accountId, UUID.randomUUID());

    Map<String, Object> review =
        jdbcTemplate.queryForMap(
            """
            SELECT
              "manualReviewStatus",
              "manualReviewedByUserId",
              "manualReviewedAt"
            FROM "BusinessAccounts"
            WHERE "id" = ?
            """,
            accountId);
    assertThat(review)
        .containsEntry("manualReviewStatus", null)
        .containsEntry("manualReviewedByUserId", null)
        .containsEntry("manualReviewedAt", null);
  }

  @Test
  void revalidationCancelsPreviousOpenDocumentRequest() {
    UUID accountId = insertBusinessAccount("ES", "B-12345674", "B12345674");
    verificationService.verify(
        new RemoteBusinessVerificationCommand(UUID.randomUUID(), accountId, null));
    UUID previousRequestId = documentRequestService.findOpen(accountId).orElseThrow().requestId();

    verificationStateService.beginRemoteCheck(accountId, UUID.randomUUID());

    assertThat(documentRequestService.findOpen(accountId)).isEmpty();
    Map<String, Object> cancelled =
        jdbcTemplate.queryForMap(
            """
            SELECT "status", "resolvedAt"
            FROM "BusinessVerificationDocumentRequests"
            WHERE "id" = ?
            """,
            previousRequestId);
    assertThat(cancelled.get("status")).isEqualTo("cancelled");
    assertThat(cancelled.get("resolvedAt")).isNotNull();
  }

  private UUID insertBusinessAccount(String country) {
    String uniqueIdentifier =
        "TEST" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    return insertBusinessAccount(country, uniqueIdentifier, uniqueIdentifier);
  }

  private UUID insertBusinessAccount(
      String country, String submittedIdentifier, String normalizedIdentifier) {
    UUID ownerId = insertTrackedUser("venue_business");
    UUID accountId =
        jdbcTemplate.queryForObject(
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
            country,
            submittedIdentifier,
            normalizedIdentifier);
    createdAccountIds.add(accountId);
    return accountId;
  }

  private UUID insertTrackedUser(String accountType) {
    String email = UUID.randomUUID() + "@example.com";
    UUID userId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO "Users" ("email", "emailNormalized", "passwordHash", "accountType")
            VALUES (?, ?, ?, ?)
            RETURNING "id"
            """,
            UUID.class,
            email,
            email,
            PASSWORD_HASH,
            accountType);
    createdOwnerIds.add(userId);
    return userId;
  }

  @TestConfiguration
  static class AdapterConfiguration {

    @Bean
    DeterministicRemoteAdapter deterministicRemoteAdapter() {
      return new DeterministicRemoteAdapter();
    }
  }

  static final class DeterministicRemoteAdapter implements RemoteBusinessVerificationAdapter {

    private final AtomicInteger invocations = new AtomicInteger();
    private final AtomicInteger failuresBeforeSuccess = new AtomicInteger();
    private RemoteVerificationStatus resultStatus = RemoteVerificationStatus.VERIFIED;
    private Boolean matchedLegalName = true;

    void reset() {
      invocations.set(0);
      failuresBeforeSuccess.set(0);
      resultStatus = RemoteVerificationStatus.VERIFIED;
      matchedLegalName = true;
    }

    @Override
    public String providerCode() {
      return "official-test";
    }

    @Override
    public Set<String> supportedCountries() {
      return Set.of("ZZ");
    }

    @Override
    public int priority() {
      return 0;
    }

    @Override
    public RemoteBusinessVerificationResult verify(
        RemoteBusinessVerificationRequest request, RemoteVerificationAttemptContext context)
        throws RemoteBusinessVerificationException {
      invocations.incrementAndGet();
      if (failuresBeforeSuccess.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
        throw new RemoteBusinessVerificationException(
            RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE);
      }
      return new RemoteBusinessVerificationResult(
          resultStatus,
          matchedLegalName,
          null,
          "TEST-" + request.requestId(),
          Instant.parse("2026-06-28T12:00:00Z"),
          "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
    }
  }
}
