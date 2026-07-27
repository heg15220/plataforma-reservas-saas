package com.reserly.platform.administration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Verifica el contrato físico de auditoría sin iniciar PostgreSQL. */
class AuditLogPersistenceTests {

  @Test
  void migrationDefinesActorSnapshotsAndOperationalIndexes() throws IOException {
    String migration = migration();

    assertThat(migration)
        .contains("CREATE TABLE \"AuditLogs\"")
        .contains("CONSTRAINT \"fkAuditLogsActorUser\"")
        .contains("CONSTRAINT \"ckAuditLogsActorRole\"")
        .contains("CONSTRAINT \"ckAuditLogsBeforeObject\"")
        .contains("CONSTRAINT \"ckAuditLogsAfterObject\"")
        .contains("CREATE INDEX \"ixAuditLogsEntityCreatedAt\"")
        .contains("CREATE INDEX \"ixAuditLogsActorCreatedAt\"");
  }

  @Test
  void entityMapsUpperCamelTableAndLowerCamelColumns() throws Exception {
    assertThat(AuditLogEntity.class.getAnnotation(Table.class).name())
        .isEqualTo("\"AuditLogs\"");
    assertThat(
            AuditLogEntity.class
                .getMethod("getActorUserId")
                .getAnnotation(Column.class)
                .name())
        .isEqualTo("\"actorUserId\"");
    assertThat(
            AuditLogEntity.class
                .getMethod("getAfterJson")
                .getAnnotation(Column.class)
                .name())
        .isEqualTo("\"afterJson\"");
  }

  private String migration() throws IOException {
    try (InputStream input =
        getClass().getResourceAsStream("/db/migration/V28__create_audit_logs.sql")) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
