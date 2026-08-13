package com.reserly.platform.demand.privacy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Propaga derechos sobre identidades y derivados seudónimos.
 *
 * <p>Supresión elimina eventos y peticiones antes que la identidad; rankings/candidatos caen por
 * cascada. Oposición y revocación impiden uso futuro sin destruir evidencia legal. El registro de
 * la solicitud solo conserva UUID opaco y contadores, nunca HMAC, contexto ni contenido de eventos.
 */
@Service
public class DemandPrivacyService {
  private static final Duration AUDIT_RETENTION = Duration.ofDays(1095);
  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public DemandPrivacyService(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Ejecuta una acción una vez; reintentos devuelven exactamente el resultado persistido. */
  @Transactional
  public DemandPrivacyResponse execute(DemandPrivacyRequest request) {
    jdbc.queryForObject(
        "SELECT pg_advisory_xact_lock(?)",
        Object.class,
        request.requestId().getMostSignificantBits()
            ^ request.requestId().getLeastSignificantBits());
    DemandPrivacyResponse previous = findCompleted(request.requestId());
    if (previous != null) return previous;
    validate(request);
    Instant now = clock.instant();
    boolean found = identityExists(request);
    Map<String, Object> result = found ? apply(request, now) : Map.of("identityFound", false);
    String status = found ? "completed" : "not_found";
    jdbc.update(
        """
          INSERT INTO "DemandPrivacyRequests" (
            "requestId", "subjectType", "subjectId", "action", "purpose", "status",
            "resultJson", "requestedAt", "completedAt", "retentionExpiresAt", "createdAt"
          ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
          """,
        request.requestId(),
        request.subjectType(),
        request.subjectId(),
        request.action(),
        request.purpose(),
        status,
        json(result),
        timestamp(now),
        timestamp(now),
        timestamp(now.plus(AUDIT_RETENTION)),
        timestamp(now));
    return new DemandPrivacyResponse(request.requestId(), status, request.action(), result, now);
  }

  private Map<String, Object> apply(DemandPrivacyRequest request, Instant now) {
    return switch (request.action()) {
      case "access" -> access(request);
      case "correction" -> correction(request, now);
      case "objection" -> revoke(request, now, null);
      case "revocation" -> revoke(request, now, request.purpose());
      case "unlink" -> unlink(request, now);
      case "erasure" -> erase(request);
      default -> throw new IllegalStateException("Acción no soportada");
    };
  }

  private Map<String, Object> access(DemandPrivacyRequest request) {
    Map<String, Object> result = base();
    result.put("events", count("BehaviorEvents", identityColumn(request), request.subjectId()));
    result.put(
        "recommendationRequests",
        count("RecommendationRequests", identityColumn(request), request.subjectId()));
    // No existe perfil personal materializado en la fase 19; se explicita el contrato para que
    // futuros perfiles vinculados entren en derechos antes de habilitarse en producción.
    result.put("profiles", 0);
    result.put("links", count("IdentityLinks", linkColumn(request), request.subjectId()));
    return result;
  }

  private Map<String, Object> correction(DemandPrivacyRequest request, Instant now) {
    int updated =
        jdbc.update(
            """
            UPDATE "CustomerIdentities"
            SET "emailHmac" = ?, "keyVersion" = ?, "updatedAt" = ? WHERE "id" = ?
            """,
            request.replacementEmailHmac(),
            request.replacementKeyVersion(),
            timestamp(now),
            request.subjectId());
    Map<String, Object> result = base();
    result.put("corrected", updated == 1);
    return result;
  }

  private Map<String, Object> revoke(DemandPrivacyRequest request, Instant now, String purpose) {
    String sql =
        "UPDATE \"IdentityLinks\" SET \"revokedAt\" = ? WHERE \""
            + linkColumn(request)
            + "\" = ? AND \"revokedAt\" IS NULL"
            + (purpose == null ? "" : " AND \"purpose\" = ?");
    int links =
        purpose == null
            ? jdbc.update(sql, timestamp(now), request.subjectId())
            : jdbc.update(sql, timestamp(now), request.subjectId(), purpose);
    boolean personalization = purpose == null || "personalization".equals(purpose);
    if (personalization) revokeIdentityConsent(request, now);
    Map<String, Object> result = base();
    result.put("linksRevoked", links);
    result.put("consentRevoked", personalization);
    return result;
  }

  private void revokeIdentityConsent(DemandPrivacyRequest request, Instant now) {
    String suffix = "customer".equals(request.subjectType()) ? ", \"updatedAt\" = ?" : "";
    String sql =
        "UPDATE \""
            + identityTable(request)
            + "\" SET \"personalizationRevokedAt\" = ?"
            + suffix
            + " WHERE \"id\" = ? AND \"personalizationConsentedAt\" IS NOT NULL"
            + " AND \"personalizationRevokedAt\" IS NULL";
    if (suffix.isEmpty()) {
      jdbc.update(sql, timestamp(now), request.subjectId());
    } else {
      jdbc.update(sql, timestamp(now), timestamp(now), request.subjectId());
    }
  }

  private Map<String, Object> unlink(DemandPrivacyRequest request, Instant now) {
    int links =
        jdbc.update(
            "UPDATE \"IdentityLinks\" SET \"revokedAt\" = ? WHERE \""
                + linkColumn(request)
                + "\" = ? AND \"revokedAt\" IS NULL",
            timestamp(now),
            request.subjectId());
    Map<String, Object> result = base();
    result.put("linksRevoked", links);
    return result;
  }

  private Map<String, Object> erase(DemandPrivacyRequest request) {
    int events = delete("BehaviorEvents", identityColumn(request), request.subjectId());
    int recommendations =
        delete("RecommendationRequests", identityColumn(request), request.subjectId());
    delete("IdentityLinks", linkColumn(request), request.subjectId());
    int identity = delete(identityTable(request), "id", request.subjectId());
    Map<String, Object> result = base();
    result.put("eventsDeleted", events);
    result.put("recommendationRequestsDeleted", recommendations);
    result.put("profilesDeleted", 0);
    result.put("identityDeleted", identity == 1);
    return result;
  }

  private void validate(DemandPrivacyRequest request) {
    boolean correction = "correction".equals(request.action());
    if (correction
        && (!"customer".equals(request.subjectType())
            || request.replacementEmailHmac() == null
            || request.replacementKeyVersion() == null)) {
      throw badRequest("correction requiere customer y HMAC/version sustitutos");
    }
    if (!correction
        && (request.replacementEmailHmac() != null || request.replacementKeyVersion() != null)) {
      throw badRequest("Solo correction admite sustitución");
    }
    if ("revocation".equals(request.action()) && request.purpose() == null) {
      throw badRequest("revocation requiere finalidad");
    }
  }

  private boolean identityExists(DemandPrivacyRequest request) {
    return count(identityTable(request), "id", request.subjectId()) == 1;
  }

  private int count(String table, String column, UUID id) {
    Integer value =
        jdbc.queryForObject(
            "SELECT count(*) FROM \"" + table + "\" WHERE \"" + column + "\" = ?",
            Integer.class,
            id);
    return value == null ? 0 : value;
  }

  private int delete(String table, String column, UUID id) {
    return jdbc.update("DELETE FROM \"" + table + "\" WHERE \"" + column + "\" = ?", id);
  }

  private String identityTable(DemandPrivacyRequest request) {
    return "customer".equals(request.subjectType()) ? "CustomerIdentities" : "AnonymousIdentities";
  }

  private String identityColumn(DemandPrivacyRequest request) {
    return "customer".equals(request.subjectType()) ? "customerIdentityId" : "anonymousIdentityId";
  }

  private String linkColumn(DemandPrivacyRequest request) {
    return identityColumn(request);
  }

  private Map<String, Object> base() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("identityFound", true);
    return result;
  }

  private String json(Map<String, Object> result) {
    try {
      return objectMapper.writeValueAsString(result);
    } catch (JacksonException exception) {
      throw new IllegalStateException("No se pudo serializar el resultado minimizado", exception);
    }
  }

  @SuppressWarnings("unchecked")
  private DemandPrivacyResponse findCompleted(UUID requestId) {
    return jdbc.query(
        """
        SELECT "requestId", "status", "action", "resultJson"::text, "completedAt"
        FROM "DemandPrivacyRequests" WHERE "requestId" = ?
        """,
        resultSet -> {
          if (!resultSet.next()) return null;
          try {
            Map<String, Object> result = objectMapper.readValue(resultSet.getString(4), Map.class);
            return new DemandPrivacyResponse(
                resultSet.getObject(1, UUID.class),
                resultSet.getString(2),
                resultSet.getString(3),
                result,
                resultSet.getTimestamp(5).toInstant());
          } catch (JacksonException exception) {
            throw new IllegalStateException("Resultado persistido inválido", exception);
          }
        },
        requestId);
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private Timestamp timestamp(Instant value) {
    return Timestamp.from(value);
  }
}
