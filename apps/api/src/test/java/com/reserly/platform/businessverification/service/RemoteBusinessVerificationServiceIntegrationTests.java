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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
@Import(RemoteBusinessVerificationServiceIntegrationTests.AdapterConfiguration.class)
class RemoteBusinessVerificationServiceIntegrationTests {

  private static final String PASSWORD_HASH = "$2a$12$placeholder.hash.for.persistence.contract";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private RemoteBusinessVerificationService verificationService;

  @Autowired private DeterministicRemoteAdapter adapter;

  @BeforeEach
  void resetAdapter() {
    adapter.reset();
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
    assertThat(repeated.verificationCheckId()).isEqualTo(first.verificationCheckId());
    assertThat(adapter.invocations).hasValue(2);

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
        .containsEntry("identifierChecked", "TEST123")
        .containsEntry("status", "verified")
        .containsEntry("remoteReference", "TEST-REMOTE-REFERENCE")
        .containsEntry("errorCode", null)
        .containsEntry("errorMessageKey", null);
    assertThat(((Number) evidence.get("attemptCount")).intValue()).isEqualTo(2);
    assertThat((Integer) evidence.get("durationMs")).isGreaterThanOrEqualTo(0);
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
    assertThat(adapter.invocations).hasValue(0);
  }

  private UUID insertBusinessAccount(String country) {
    return insertBusinessAccount(country, "TEST-123", "TEST123");
  }

  private UUID insertBusinessAccount(
      String country, String submittedIdentifier, String normalizedIdentifier) {
    String email = UUID.randomUUID() + "@example.com";
    UUID ownerId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO "Users" ("email", "emailNormalized", "passwordHash", "accountType")
            VALUES (?, ?, ?, 'venue_business')
            RETURNING "id"
            """,
            UUID.class,
            email,
            email,
            PASSWORD_HASH);
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
        country,
        submittedIdentifier,
        normalizedIdentifier);
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

    void reset() {
      invocations.set(0);
      failuresBeforeSuccess.set(0);
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
          RemoteVerificationStatus.VERIFIED,
          true,
          null,
          "TEST-REMOTE-REFERENCE",
          Instant.parse("2026-06-28T12:00:00Z"),
          "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
    }
  }
}
