package com.reserly.platform.demand.retention;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ejecuta lotes pequeños y ordenados para evitar transacciones largas.
 *
 * <p>La petición de recomendación es raíz: su borrado elimina candidatos/rankings por cascada. Las
 * identidades se retiran después de links y derivados; las FK restantes usan SET NULL. No se
 * incluyen tablas operativas de reserva, auditoría legal general ni pagos.
 */
@Service
public class DemandRetentionService {
  private final JdbcTemplate jdbc;
  private final DemandRetentionProperties properties;
  private final Clock clock;

  public DemandRetentionService(
      JdbcTemplate jdbc, DemandRetentionProperties properties, Clock clock) {
    this.jdbc = jdbc;
    this.properties = properties;
    this.clock = clock;
  }

  /** Borra como máximo batchSize filas por agregado y devuelve solo contadores. */
  @Transactional
  public DemandRetentionResult runOnce() {
    Instant now = clock.instant();
    int batch = properties.batchSize();
    int events = deleteExpired("BehaviorEvents", "retentionExpiresAt", now, batch);
    int recommendations = deleteExpired("RecommendationRequests", "retentionExpiresAt", now, batch);
    int profiles = deleteExpired("VenueAttributeProfiles", "expiresAt", now, batch);
    int evidences = deleteExpired("VenueAttributeEvidences", "expiresAt", now, batch);
    int links = deleteExpired("IdentityLinks", "retentionExpiresAt", now, batch);
    int anonymous = deleteUnreferencedIdentity("AnonymousIdentities", now, batch);
    int customers = deleteUnreferencedIdentity("CustomerIdentities", now, batch);
    int audits = deleteExpired("DemandPrivacyRequests", "retentionExpiresAt", now, batch);
    return new DemandRetentionResult(
        events, recommendations, profiles, evidences, links, anonymous, customers, audits);
  }

  private int deleteExpired(String table, String column, Instant now, int batch) {
    return jdbc.update(
        "WITH victims AS (SELECT ctid FROM \""
            + table
            + "\" WHERE \""
            + column
            + "\" IS NOT NULL AND \""
            + column
            + "\" <= ? ORDER BY \""
            + column
            + "\" LIMIT ?) DELETE FROM \""
            + table
            + "\" target USING victims WHERE target.ctid = victims.ctid",
        Timestamp.from(now),
        batch);
  }

  private int deleteUnreferencedIdentity(String table, Instant now, int batch) {
    String identityColumn =
        "CustomerIdentities".equals(table) ? "customerIdentityId" : "anonymousIdentityId";
    return jdbc.update(
        "WITH victims AS (SELECT identity.ctid FROM \""
            + table
            + "\" identity WHERE identity.\"retentionExpiresAt\" <= ?"
            + " AND NOT EXISTS (SELECT 1 FROM \"IdentityLinks\" link WHERE link.\""
            + identityColumn
            + "\" = identity.\"id\") ORDER BY identity.\"retentionExpiresAt\" LIMIT ?)"
            + " DELETE FROM \""
            + table
            + "\" target USING victims WHERE target.ctid = victims.ctid",
        Timestamp.from(now),
        batch);
  }
}
