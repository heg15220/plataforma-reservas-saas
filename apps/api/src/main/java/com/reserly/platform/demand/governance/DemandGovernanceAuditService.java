package com.reserly.platform.demand.governance;

import com.reserly.platform.administration.persistence.AuditLogDao;
import com.reserly.platform.administration.persistence.AuditLogEntity;
import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra gobierno de demanda sobre el ledger administrativo común.
 *
 * <p>Solo persiste códigos, versiones, hashes, vigencia y correlación. Rechaza texto libre,
 * payloads, métricas, identidades de clientes y combinaciones actor/automatización ambiguas. Un
 * reintento con el mismo eventId devuelve la entrada previa únicamente si el contrato coincide
 * exactamente.
 */
@Service
public class DemandGovernanceAuditService {
  private static final Set<String> ARTIFACT_RESOURCES =
      Set.of("ontology", "ranking_weights", "model", "experiment", "promotion");
  private static final Map<String, Set<String>> ACTIONS =
      Map.of(
          "ontology", Set.of("proposed", "approved", "merged", "retired", "rejected"),
          "ranking_weights", Set.of("created", "updated", "activated", "rolled_back"),
          "model", Set.of("registered", "shadowed", "canary_started", "retired", "rolled_back"),
          "experiment", Set.of("created", "started", "stopped", "evaluated"),
          "promotion", Set.of("requested", "approved", "rejected", "executed", "rolled_back"),
          "waitlist", Set.of("allocated", "offer_issued", "accepted", "expired", "cancelled"),
          "automatic_action", Set.of("triggered", "completed", "failed", "rolled_back"));

  private final AuditLogDao auditLogDao;
  private final AuditLogService auditLogService;

  public DemandGovernanceAuditService(AuditLogDao auditLogDao, AuditLogService auditLogService) {
    this.auditLogDao = auditLogDao;
    this.auditLogService = auditLogService;
  }

  /**
   * Persiste o reusa un evento autenticado por servicio sin conceder autoridad sobre el recurso.
   */
  @Transactional
  public DemandGovernanceAuditResponse recordSystem(
      String sourceServiceId, DemandGovernanceAuditRequest request) {
    validate(sourceServiceId, request);
    String entityType = "demand_" + request.resourceType();
    Map<String, Object> before = snapshotBefore(request);
    Map<String, Object> after = snapshotAfter(sourceServiceId, request);
    auditLogDao.lockDemandGovernanceEvent(request.eventId());
    return auditLogDao
        .findByEntityTypeAndEntityId(entityType, request.eventId())
        .map(existing -> replay(existing, request, before, after))
        .orElseGet(
            () ->
                response(
                    auditLogService.record(
                        new AuditLogEntry(
                            null,
                            "system",
                            entityType,
                            request.eventId(),
                            "governance." + request.action(),
                            before,
                            after,
                            null,
                            null)),
                    request));
  }

  private DemandGovernanceAuditResponse replay(
      AuditLogEntity existing,
      DemandGovernanceAuditRequest request,
      Map<String, Object> before,
      Map<String, Object> after) {
    if (!existing.getAction().equals("governance." + request.action())
        || !java.util.Objects.equals(existing.getBeforeJson(), before)
        || !java.util.Objects.equals(existing.getAfterJson(), after)) {
      throw new IllegalArgumentException("DEMAND_GOVERNANCE_EVENT_ID_CONFLICT");
    }
    return response(existing, request);
  }

  private DemandGovernanceAuditResponse response(
      AuditLogEntity entity, DemandGovernanceAuditRequest request) {
    return new DemandGovernanceAuditResponse(
        entity.getId(),
        request.eventId(),
        request.resourceType(),
        request.action(),
        entity.getCreatedAt());
  }

  private void validate(String sourceServiceId, DemandGovernanceAuditRequest request) {
    if (request == null
        || !token(sourceServiceId, 64)
        || request.resourceType() == null
        || !ACTIONS.containsKey(request.resourceType())
        || !ACTIONS.get(request.resourceType()).contains(request.action())
        || !token(request.resourceKey(), 128)
        || !reasonCode(request.reasonCode())
        || !token(request.policyVersion(), 64)
        || request.eventId() == null
        || request.correlationId() == null
        || request.effectiveFrom() == null
        || request.automated() == null
        || (request.beforeVersion() == null && request.afterVersion() == null)
        || (request.effectiveUntil() != null
            && !request.effectiveUntil().isAfter(request.effectiveFrom()))
        || (ARTIFACT_RESOURCES.contains(request.resourceType())
            && !sha256(request.artifactSha256()))
        || (request.artifactSha256() != null && !sha256(request.artifactSha256()))
        || (ARTIFACT_RESOURCES.contains(request.resourceType())
            && Boolean.TRUE.equals(request.automated()))
        || (Boolean.FALSE.equals(request.automated()) && !token(request.approvalReference(), 64))
        || (Boolean.TRUE.equals(request.automated()) && request.approvalReference() != null)
        || ("automatic_action".equals(request.resourceType())
            && !Boolean.TRUE.equals(request.automated()))) {
      throw new IllegalArgumentException("DEMAND_GOVERNANCE_AUDIT_INVALID");
    }
  }

  private Map<String, Object> snapshotBefore(DemandGovernanceAuditRequest request) {
    if (request.beforeVersion() == null) {
      return null;
    }
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("resourceKey", request.resourceKey());
    snapshot.put("version", request.beforeVersion());
    snapshot.put("policyVersion", request.policyVersion());
    return Map.copyOf(snapshot);
  }

  private Map<String, Object> snapshotAfter(
      String sourceServiceId, DemandGovernanceAuditRequest request) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("resourceKey", request.resourceKey());
    if (request.afterVersion() != null) {
      snapshot.put("version", request.afterVersion());
    }
    snapshot.put("policyVersion", request.policyVersion());
    snapshot.put("reasonCode", request.reasonCode());
    snapshot.put("effectiveFrom", request.effectiveFrom().toString());
    if (request.effectiveUntil() != null) {
      snapshot.put("effectiveUntil", request.effectiveUntil().toString());
    }
    if (request.artifactSha256() != null) {
      snapshot.put("artifactSha256", request.artifactSha256());
    }
    if (request.approvalReference() != null) {
      snapshot.put("approvalReference", request.approvalReference());
    }
    snapshot.put("automated", request.automated());
    snapshot.put("correlationId", request.correlationId().toString());
    snapshot.put("sourceServiceId", sourceServiceId);
    return Map.copyOf(snapshot);
  }

  private boolean token(String value, int maximumLength) {
    return value != null
        && !value.isBlank()
        && value.length() <= maximumLength
        && value.matches("^[a-z][A-Za-z0-9._:-]*$");
  }

  private boolean sha256(String value) {
    return value != null && value.matches("^[a-f0-9]{64}$");
  }

  private boolean reasonCode(String value) {
    return value != null && value.matches("^[a-z][a-z0-9._-]{0,63}$");
  }
}
