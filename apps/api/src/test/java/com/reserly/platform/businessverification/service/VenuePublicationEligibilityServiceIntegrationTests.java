package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Verifica la frontera transaccional de publicación contra PostgreSQL real. */
@SpringBootTest
@ActiveProfiles("test")
class VenuePublicationEligibilityServiceIntegrationTests {

  private static final String PASSWORD_HASH = "$2a$12$placeholder.hash.for.persistence.contract";

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private VenuePublicationEligibilityService service;

  private final List<UUID> accountIds = new ArrayList<>();
  private final List<UUID> userIds = new ArrayList<>();

  @AfterEach
  void removeCommittedFixtures() {
    accountIds.forEach(
        accountId ->
            jdbcTemplate.update(
                """
                DELETE FROM "BusinessAccounts"
                WHERE "id" = ?
                """,
                accountId));
    userIds.forEach(
        userId ->
            jdbcTemplate.update(
                """
                DELETE FROM "Users"
                WHERE "id" = ?
                """,
                userId));
    accountIds.clear();
    userIds.clear();
  }

  @Test
  void blocksUnverifiedEmailAndBusinessAccount() {
    UUID ownerId = insertUser("venue_business", null);
    UUID accountId = insertUnverifiedAccount(ownerId);

    VenuePublicationEligibility decision = service.evaluate(accountId);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.blockers())
        .containsExactlyInAnyOrder(
            VenuePublicationBlocker.EMAIL_NOT_VERIFIED,
            VenuePublicationBlocker.BUSINESS_VERIFICATION_NOT_APPROVED);
    assertThatThrownBy(() -> service.requireEligible(accountId))
        .isInstanceOf(VenuePublicationNotAllowedException.class);
  }

  @Test
  void allowsVerifiedEmailAndUnexpiredRemoteApproval() {
    UUID ownerId = insertUser("venue_business", Instant.now().minusSeconds(60));
    UUID accountId = insertVerifiedAccount(ownerId);

    assertThat(service.evaluate(accountId).allowed()).isTrue();
    service.requireEligible(accountId);
  }

  @Test
  void allowsAuditedManualApprovalAndRejectsUnknownAccountGenerically() {
    UUID ownerId = insertUser("venue_business", Instant.now().minusSeconds(60));
    UUID reviewerId = insertUser("admin", Instant.now().minusSeconds(60));
    UUID accountId = insertManuallyApprovedAccount(ownerId, reviewerId);

    assertThat(service.evaluate(accountId).allowed()).isTrue();
    assertThatThrownBy(() -> service.evaluate(UUID.randomUUID()))
        .isInstanceOf(VenuePublicationNotAllowedException.class);
  }

  private UUID insertUser(String accountType, Instant emailVerifiedAt) {
    String email = UUID.randomUUID() + "@publication.test";
    UUID userId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO "Users" (
              "email",
              "emailNormalized",
              "passwordHash",
              "accountType",
              "emailVerifiedAt"
            )
            VALUES (?, ?, ?, ?, ?)
            RETURNING "id"
            """,
            UUID.class,
            email,
            email,
            PASSWORD_HASH,
            accountType,
            timestamp(emailVerifiedAt));
    userIds.add(userId);
    return userId;
  }

  private UUID insertUnverifiedAccount(UUID ownerId) {
    return insertAccount(ownerId, "unverified", null, null, null, null);
  }

  private UUID insertVerifiedAccount(UUID ownerId) {
    Instant verifiedAt = Instant.now().minusSeconds(60);
    return insertAccount(
        ownerId, "verified", verifiedAt, verifiedAt.plusSeconds(86_400), null, null);
  }

  private UUID insertManuallyApprovedAccount(UUID ownerId, UUID reviewerId) {
    return insertAccount(ownerId, "pending_review", null, null, "approved", reviewerId);
  }

  private UUID insertAccount(
      UUID ownerId,
      String verificationStatus,
      Instant verifiedAt,
      Instant expiresAt,
      String manualReviewStatus,
      UUID reviewerId) {
    String taxIdentifier = "TEST" + UUID.randomUUID().toString().replace("-", "");
    UUID accountId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO "BusinessAccounts" (
              "ownerUserId",
              "taxCountry",
              "businessLegalName",
              "businessTaxIdentifier",
              "businessTaxIdentifierNormalized",
              "businessVerificationStatus",
              "businessVerifiedAt",
              "businessVerificationExpiresAt",
              "manualReviewStatus",
              "manualReviewedByUserId",
              "manualReviewedAt"
            )
            VALUES (?, 'ZZ', 'Empresa elegibilidad', ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING "id"
            """,
            UUID.class,
            ownerId,
            taxIdentifier,
            taxIdentifier,
            verificationStatus,
            timestamp(verifiedAt),
            timestamp(expiresAt),
            manualReviewStatus,
            reviewerId,
            reviewerId == null ? null : Timestamp.from(Instant.now()));
    accountIds.add(accountId);
    return accountId;
  }

  private Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }
}
