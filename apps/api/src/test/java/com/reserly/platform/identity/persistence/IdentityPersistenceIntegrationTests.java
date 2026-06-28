package com.reserly.platform.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.identity.AccountType;
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
 * Verifica el contrato físico y las invariantes de las tablas de identidad sobre PostgreSQL real.
 *
 * <p>Estos tests complementan la validación de Hibernate: comprueban seeds, unicidad, hashes,
 * relaciones y cascadas que no pueden demostrarse solo arrancando el contexto JPA.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityPersistenceIntegrationTests {

  private static final String PASSWORD_HASH = "$2a$12$placeholder.hash.for.persistence.contract";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private UserDao userDao;

  @Autowired private RoleDao roleDao;

  @Autowired private UserRoleDao userRoleDao;

  @Autowired private AuthSessionDao authSessionDao;

  @Autowired private AuthTokenDao authTokenDao;

  @Test
  void exposesAllIdentityRepositoriesAndSeedsAssignableRoles() {
    assertThat(userDao).isNotNull();
    assertThat(userRoleDao).isNotNull();
    assertThat(authSessionDao).isNotNull();
    assertThat(authTokenDao).isNotNull();

    List<String> roleCodes =
        jdbcTemplate.queryForList(
            """
            SELECT "code"
            FROM "Roles"
            ORDER BY "code"
            """,
            String.class);

    assertThat(roleDao.count()).isEqualTo(3);
    assertThat(roleCodes).containsExactly("admin", "employee_user", "venue_owner");
  }

  @Test
  void createsExpectedUpperCamelCaseTables() {
    List<String> tables =
        jdbcTemplate.queryForList(
            """
            SELECT "table_name"
            FROM "information_schema"."tables"
            WHERE "table_schema" = 'public'
              AND "table_name" IN ('Users', 'Roles', 'UserRoles', 'AuthSessions', 'AuthTokens')
            ORDER BY "table_name"
            """,
            String.class);

    assertThat(tables).containsExactly("AuthSessions", "AuthTokens", "Roles", "UserRoles", "Users");
  }

  @Test
  void rejectsDuplicateNormalizedEmail() {
    insertUser("Owner@Example.com", "owner@example.com");

    assertThatThrownBy(() -> insertUser("OWNER@example.com", "owner@example.com"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uqUsersEmailNormalized");
  }

  @Test
  void defaultsToCustomerAndConvertsEverySupportedAccountType() {
    UUID customerId = insertUser("customer@example.com", "customer@example.com");
    UUID venueId =
        insertUser(
            "venue@example.com", "venue@example.com", AccountType.VENUE_BUSINESS.persistedValue());
    UUID adminId =
        insertUser("admin@example.com", "admin@example.com", AccountType.ADMIN.persistedValue());

    assertThat(userDao.findById(customerId).orElseThrow().getAccountType())
        .isEqualTo(AccountType.CUSTOMER);
    assertThat(userDao.findById(venueId).orElseThrow().getAccountType())
        .isEqualTo(AccountType.VENUE_BUSINESS);
    assertThat(userDao.findById(adminId).orElseThrow().getAccountType())
        .isEqualTo(AccountType.ADMIN);

    UserEntity customer = userDao.findById(customerId).orElseThrow();
    customer.setAccountType(AccountType.VENUE_BUSINESS);
    userDao.saveAndFlush(customer);

    String persistedValue =
        jdbcTemplate.queryForObject(
            """
            SELECT "accountType"
            FROM "Users"
            WHERE "id" = ?
            """,
            String.class,
            customerId);
    assertThat(persistedValue).isEqualTo("venue_business");
  }

  @Test
  void rejectsAccountTypesOutsideTheClosedCatalog() {
    assertThatThrownBy(
            () ->
                insertUser(
                    "unknown@example.com", "unknown@example.com", "external_business_partner"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckUsersAccountType");
  }

  @Test
  void rejectsSessionSecretsThatAreNotSha256HexHashes() {
    UUID userId = insertUser("owner@example.com", "owner@example.com");
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "AuthSessions"
                      ("userId", "tokenHash", "createdAt", "lastSeenAt", "expiresAt")
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    userId,
                    "raw-session-secret",
                    Timestamp.from(now),
                    Timestamp.from(now),
                    Timestamp.from(now.plus(1, ChronoUnit.HOURS))))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckAuthSessionsTokenHash");
  }

  @Test
  void cascadesCredentialsAndAssignmentsWhenUserIsDeleted() {
    UUID userId = insertUser("owner@example.com", "owner@example.com");
    UUID roleId =
        jdbcTemplate.queryForObject(
            """
            SELECT "id"
            FROM "Roles"
            WHERE "code" = 'venue_owner'
            """,
            UUID.class);
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    jdbcTemplate.update(
        """
        INSERT INTO "UserRoles" ("userId", "roleId")
        VALUES (?, ?)
        """,
        userId,
        roleId);
    jdbcTemplate.update(
        """
        INSERT INTO "AuthSessions"
          ("userId", "tokenHash", "createdAt", "lastSeenAt", "expiresAt")
        VALUES (?, ?, ?, ?, ?)
        """,
        userId,
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        Timestamp.from(now),
        Timestamp.from(now),
        Timestamp.from(now.plus(1, ChronoUnit.HOURS)));
    jdbcTemplate.update(
        """
        INSERT INTO "AuthTokens" ("userId", "purpose", "tokenHash", "createdAt", "expiresAt")
        VALUES (?, 'email_verification', ?, ?, ?)
        """,
        userId,
        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
        Timestamp.from(now),
        Timestamp.from(now.plus(15, ChronoUnit.MINUTES)));

    jdbcTemplate.update(
        """
        DELETE FROM "Users"
        WHERE "id" = ?
        """,
        userId);

    assertThat(countRowsForUser("UserRoles", userId)).isZero();
    assertThat(countRowsForUser("AuthSessions", userId)).isZero();
    assertThat(countRowsForUser("AuthTokens", userId)).isZero();
  }

  private UUID insertUser(String email, String normalizedEmail) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "Users" ("email", "emailNormalized", "passwordHash")
        VALUES (?, ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        email,
        normalizedEmail,
        PASSWORD_HASH);
  }

  private UUID insertUser(String email, String normalizedEmail, String accountType) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "Users" ("email", "emailNormalized", "passwordHash", "accountType")
        VALUES (?, ?, ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        email,
        normalizedEmail,
        PASSWORD_HASH,
        accountType);
  }

  private int countRowsForUser(String quotedTableName, UUID userId) {
    String sql =
        switch (quotedTableName) {
          case "UserRoles" -> "SELECT count(*) FROM \"UserRoles\" WHERE \"userId\" = ?";
          case "AuthSessions" -> "SELECT count(*) FROM \"AuthSessions\" WHERE \"userId\" = ?";
          case "AuthTokens" -> "SELECT count(*) FROM \"AuthTokens\" WHERE \"userId\" = ?";
          default -> throw new IllegalArgumentException("Unsupported identity table");
        };

    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
    return count == null ? 0 : count;
  }
}
