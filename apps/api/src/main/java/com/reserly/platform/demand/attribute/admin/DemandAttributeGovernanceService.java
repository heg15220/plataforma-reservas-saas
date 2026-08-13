package com.reserly.platform.demand.attribute.admin;

import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeCandidateDao;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeCandidateEntity;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeDao;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Máquina de estados de gobierno de la ontología.
 *
 * <p>Ninguna propuesta se publica por inferencia automática. Cada transición queda ligada al admin
 * autenticado y a un snapshot minimizado. Las fusiones son enlaces, nunca borrados, y las
 * decisiones terminales requieren una justificación.
 */
@Service
public class DemandAttributeGovernanceService {
  private static final Set<String> SOURCES =
      Set.of(
          "venueDeclaration",
          "structuredCatalog",
          "operational",
          "customerAggregate",
          "verifiedAudit",
          "imageAuxiliary");
  private static final Map<String, Set<String>> TRANSITIONS =
      Map.of(
          "draft", Set.of("in_review", "retired"),
          "in_review", Set.of("published", "merged", "retired", "rejected"),
          "published", Set.of("merged", "retired"));

  private final DemandAttributeDao attributeDao;
  private final DemandAttributeCandidateDao candidateDao;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public DemandAttributeGovernanceService(
      DemandAttributeDao attributeDao,
      DemandAttributeCandidateDao candidateDao,
      AuditLogService auditLogService,
      Clock clock) {
    this.attributeDao = attributeDao;
    this.candidateDao = candidateDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  /** Devuelve incluso estados terminales para hacer visible la historia de decisiones. */
  @Transactional(readOnly = true)
  public DemandAttributeAdminListResponse list() {
    return new DemandAttributeAdminListResponse(
        attributeDao.findAllByOrderByFamilyAscCodeAsc().stream().map(this::response).toList(),
        candidateDao.findAllByOrderByUpdatedAtDescProposedCodeAsc().stream()
            .map(this::response)
            .toList());
  }

  /** Crea un borrador y rechaza fuentes desconocidas o ejemplos que parezcan contener email/URL. */
  @Transactional
  public DemandAttributeCandidateAdminResponse createCandidate(
      UUID actorId, DemandAttributeCandidateRequest request, AdminRequestContext context) {
    validateSources(request.allowedSources());
    if (request.exampleSummaries().stream().anyMatch(this::containsDirectIdentifier)) {
      throw invalid("Los ejemplos deben estar minimizados y no contener email ni URL");
    }
    if (attributeDao.findByCode(request.proposedCode()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya está publicado");
    }
    Instant now = clock.instant();
    DemandAttributeCandidateEntity candidate = new DemandAttributeCandidateEntity();
    candidate.setProposedCode(request.proposedCode().strip());
    candidate.setClusterKey(request.clusterKey().strip());
    candidate.setFamily(request.family());
    candidate.setAttributeType(request.attributeType());
    candidate.setNameEs(request.nameEs().strip());
    candidate.setNameEn(request.nameEn().strip());
    candidate.setDefinitionEs(request.definitionEs().strip());
    candidate.setDefinitionEn(request.definitionEn().strip());
    candidate.setAllowedSources(request.allowedSources());
    candidate.setExampleSummaries(request.exampleSummaries().stream().map(String::strip).toList());
    candidate.setGovernanceStatus("draft");
    candidate.setCreatedAt(now);
    candidate.setUpdatedAt(now);
    candidateDao.saveAndFlush(candidate);
    audit(
        actorId,
        "demand_attribute_candidate",
        candidate.getId(),
        "candidate.created",
        null,
        snapshot(candidate),
        context);
    return response(candidate);
  }

  /** Aplica revisión, publicación, fusión, rechazo o retirada con reglas cerradas. */
  @Transactional
  public DemandAttributeCandidateAdminResponse transitionCandidate(
      UUID actorId,
      UUID candidateId,
      DemandAttributeTransitionRequest request,
      AdminRequestContext context) {
    DemandAttributeCandidateEntity candidate =
        candidateDao.findById(candidateId).orElseThrow(() -> notFound("Candidato no encontrado"));
    String previous = candidate.getGovernanceStatus();
    validateTransition(previous, request.status());
    validateDecision(request);
    Map<String, Object> before = snapshot(candidate);
    Instant now = clock.instant();
    candidate.setGovernanceStatus(request.status());
    candidate.setReviewedByUserId(actorId);
    candidate.setReviewedAt(now);
    candidate.setDecisionReason(normalize(request.reason()));
    if ("published".equals(request.status())) {
      DemandAttributeEntity attribute = publishCandidate(candidate, now);
      candidate.setResultingAttributeId(attribute.getId());
      candidate.setPublishedAt(now);
      audit(
          actorId,
          "demand_attribute",
          attribute.getId(),
          "attribute.published",
          null,
          snapshot(attribute),
          context);
    } else if ("merged".equals(request.status())) {
      DemandAttributeEntity target = publishedTarget(request.targetAttributeId());
      candidate.setResultingAttributeId(target.getId());
    } else if ("retired".equals(request.status())) {
      candidate.setRetiredAt(now);
    }
    candidate.setUpdatedAt(now);
    candidateDao.saveAndFlush(candidate);
    audit(
        actorId,
        "demand_attribute_candidate",
        candidate.getId(),
        "candidate." + request.status(),
        before,
        snapshot(candidate),
        context);
    return response(candidate);
  }

  /** Conserva el término antiguo y bloquea fusiones hacia términos no publicados. */
  @Transactional
  public DemandAttributeAdminResponse transitionAttribute(
      UUID actorId,
      UUID attributeId,
      DemandAttributeTransitionRequest request,
      AdminRequestContext context) {
    if ("rejected".equals(request.status())) {
      throw invalid("Un atributo persistido no admite rechazo; debe retirarse");
    }
    DemandAttributeEntity attribute =
        attributeDao.findById(attributeId).orElseThrow(() -> notFound("Atributo no encontrado"));
    validateTransition(attribute.getGovernanceStatus(), request.status());
    validateDecision(request);
    Map<String, Object> before = snapshot(attribute);
    Instant now = clock.instant();
    attribute.setGovernanceStatus(request.status());
    attribute.setReviewedByUserId(actorId);
    attribute.setReviewedAt(now);
    if ("published".equals(request.status())) {
      attribute.setPublishedAt(now);
    } else if ("merged".equals(request.status())) {
      DemandAttributeEntity target = publishedTarget(request.targetAttributeId());
      if (target.getId().equals(attributeId)) {
        throw invalid("Un atributo no puede fusionarse consigo mismo");
      }
      attribute.setMergedIntoId(target.getId());
    } else if ("retired".equals(request.status())) {
      attribute.setRetiredAt(now);
    }
    attribute.setUpdatedAt(now);
    attributeDao.saveAndFlush(attribute);
    audit(
        actorId,
        "demand_attribute",
        attribute.getId(),
        "attribute." + request.status(),
        before,
        snapshot(attribute),
        context);
    return response(attribute);
  }

  private DemandAttributeEntity publishCandidate(
      DemandAttributeCandidateEntity candidate, Instant now) {
    if (attributeDao.findByCode(candidate.getProposedCode()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya existe");
    }
    DemandAttributeEntity attribute = new DemandAttributeEntity();
    attribute.setOntologyVersion("personal-care.admin.v1");
    attribute.setCode(candidate.getProposedCode());
    attribute.setFamily(candidate.getFamily());
    attribute.setAttributeType(candidate.getAttributeType());
    attribute.setNameEs(candidate.getNameEs());
    attribute.setNameEn(candidate.getNameEn());
    attribute.setDefinitionEs(candidate.getDefinitionEs());
    attribute.setDefinitionEn(candidate.getDefinitionEn());
    attribute.setAllowedSources(candidate.getAllowedSources());
    attribute.setAllowedUses(List.of("profile", "ranking", "explanation"));
    attribute.setValidityMode("ttl");
    attribute.setTtlDays(180);
    attribute.setMinimumEvidence(5);
    attribute.setGovernanceStatus("published");
    attribute.setReviewedByUserId(candidate.getReviewedByUserId());
    attribute.setReviewedAt(now);
    attribute.setPublishedAt(now);
    attribute.setCreatedAt(now);
    attribute.setUpdatedAt(now);
    return attributeDao.saveAndFlush(attribute);
  }

  private DemandAttributeEntity publishedTarget(UUID targetId) {
    if (targetId == null) {
      throw invalid("La fusión requiere un atributo de destino");
    }
    DemandAttributeEntity target =
        attributeDao.findById(targetId).orElseThrow(() -> notFound("Destino no encontrado"));
    if (!"published".equals(target.getGovernanceStatus())) {
      throw invalid("El destino debe estar publicado");
    }
    return target;
  }

  private void validateTransition(String current, String target) {
    if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Transición no permitida: " + current + " -> " + target);
    }
  }

  private void validateDecision(DemandAttributeTransitionRequest request) {
    if (Set.of("merged", "retired", "rejected").contains(request.status())
        && normalize(request.reason()) == null) {
      throw invalid("La decisión terminal requiere un motivo");
    }
    if (!"merged".equals(request.status()) && request.targetAttributeId() != null) {
      throw invalid("Solo una fusión admite destino");
    }
  }

  private void validateSources(List<String> sources) {
    if (!SOURCES.containsAll(sources) || Set.copyOf(sources).size() != sources.size()) {
      throw invalid("Las fuentes deben ser conocidas y no repetirse");
    }
  }

  private boolean containsDirectIdentifier(String value) {
    String normalized = value.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("@")
        || normalized.contains("http://")
        || normalized.contains("https://");
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }

  private DemandAttributeAdminResponse response(DemandAttributeEntity item) {
    return new DemandAttributeAdminResponse(
        item.getId(),
        item.getCode(),
        item.getFamily(),
        item.getParentCode(),
        item.getAttributeType(),
        item.getNameEs(),
        item.getNameEn(),
        item.getAllowedSources(),
        item.getValidityMode(),
        item.getTtlDays(),
        item.getMinimumEvidence(),
        item.getGovernanceStatus(),
        item.getMergedIntoId(),
        item.getVersion(),
        item.getUpdatedAt());
  }

  private DemandAttributeCandidateAdminResponse response(DemandAttributeCandidateEntity item) {
    return new DemandAttributeCandidateAdminResponse(
        item.getId(),
        item.getProposedCode(),
        item.getClusterKey(),
        item.getFamily(),
        item.getAttributeType(),
        item.getNameEs(),
        item.getNameEn(),
        item.getAllowedSources(),
        item.getExampleSummaries(),
        item.getGovernanceStatus(),
        item.getDecisionReason(),
        item.getResultingAttributeId(),
        item.getVersion(),
        item.getUpdatedAt());
  }

  private Map<String, Object> snapshot(DemandAttributeEntity item) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("code", item.getCode());
    snapshot.put("family", item.getFamily());
    snapshot.put("status", item.getGovernanceStatus());
    if (item.getMergedIntoId() != null) {
      snapshot.put("mergedIntoId", item.getMergedIntoId());
    }
    snapshot.put("ontologyVersion", item.getOntologyVersion());
    return snapshot;
  }

  private Map<String, Object> snapshot(DemandAttributeCandidateEntity item) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("proposedCode", item.getProposedCode());
    snapshot.put("clusterKey", item.getClusterKey());
    snapshot.put("status", item.getGovernanceStatus());
    if (item.getResultingAttributeId() != null) {
      snapshot.put("resultingAttributeId", item.getResultingAttributeId());
    }
    return snapshot;
  }

  private void audit(
      UUID actorId,
      String entityType,
      UUID entityId,
      String action,
      Map<String, Object> before,
      Map<String, Object> after,
      AdminRequestContext context) {
    auditLogService.record(
        new AuditLogEntry(
            actorId,
            "admin",
            entityType,
            entityId,
            action,
            before,
            after,
            context.ipAddress(),
            context.userAgent()));
  }
}
