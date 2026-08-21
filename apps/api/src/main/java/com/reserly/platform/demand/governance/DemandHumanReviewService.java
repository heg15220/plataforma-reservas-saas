package com.reserly.platform.demand.governance;

import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.venues.persistence.VenueDao;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Máquina de estados para revisión previa, corrección e impugnación de impacto material.
 *
 * <p>La única condición que autoriza ejecución es {@code approved}; submission, appeal y corrección
 * nunca ejecutan la decisión comercial. Todos los motivos son códigos y la evidencia solo un
 * digest.
 */
@Service
public class DemandHumanReviewService {
  private static final Set<String> REVIEWABLE_SUBJECTS =
      Set.of(
          "attribute_candidate",
          "ranking_policy",
          "promotion",
          "waitlist_policy",
          "commercial_action");
  private static final Map<String, Set<String>> TRANSITIONS =
      Map.of(
          "submitted", Set.of("approved", "rejected", "correction_requested"),
          "appealed", Set.of("approved", "rejected", "correction_requested"),
          "correction_requested", Set.of("corrected"),
          "corrected", Set.of("approved", "rejected", "correction_requested"));

  private final DemandHumanReviewDao reviewDao;
  private final VenueDao venueDao;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public DemandHumanReviewService(
      DemandHumanReviewDao reviewDao,
      VenueDao venueDao,
      AuditLogService auditLogService,
      Clock clock) {
    this.reviewDao = reviewDao;
    this.venueDao = venueDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  /** Registra una propuesta idempotente; nunca devuelve autorización antes de revisión humana. */
  @Transactional
  public DemandHumanReviewResponse submit(
      String sourceService, DemandHumanReviewSubmissionRequest request) {
    validateSubmission(sourceService, request);
    reviewDao.lockSubmission(request.reviewId());
    return reviewDao
        .findById(request.reviewId())
        .map(existing -> replay(existing, request))
        .orElseGet(() -> create(sourceService, request));
  }

  /** Lista una cola administrativa acotada, incluidos estados terminales e impugnados. */
  @Transactional(readOnly = true)
  public List<DemandHumanReviewResponse> listAdmin() {
    return reviewDao.findAdminPage(PageRequest.of(0, 100)).stream().map(this::response).toList();
  }

  /** Aplica una decisión humana auditada bajo lock; no permite saltos de estado. */
  @Transactional
  public DemandHumanReviewResponse decide(
      UUID actorId,
      UUID reviewId,
      DemandHumanReviewDecisionRequest request,
      AdminRequestContext context) {
    DemandHumanReviewEntity entity = locked(reviewId);
    if (!TRANSITIONS.getOrDefault(entity.getStatus(), Set.of()).contains(request.status())) {
      throw conflict();
    }
    if (("corrected".equals(request.status())) != (request.correctionVersion() != null)) {
      throw invalid();
    }
    Map<String, Object> before = snapshot(entity);
    Instant now = clock.instant();
    entity.setStatus(request.status());
    entity.setReviewerUserId(actorId);
    entity.setReviewReasonCode(request.reasonCode());
    entity.setCorrectionVersion(request.correctionVersion());
    entity.setReviewedAt(now);
    entity.setUpdatedAt(now);
    reviewDao.saveAndFlush(entity);
    audit(
        actorId,
        "admin",
        entity,
        "human_review." + request.status(),
        before,
        snapshot(entity),
        context);
    return response(entity);
  }

  /** Registra una única impugnación para un local accesible y reabre la decisión. */
  @Transactional
  public DemandHumanReviewResponse appeal(
      UUID actorId,
      UUID reviewId,
      DemandHumanReviewAppealRequest request,
      AdminRequestContext context) {
    DemandHumanReviewEntity entity = locked(reviewId);
    if (entity.getVenueId() == null
        || venueDao.findAccessibleById(actorId, entity.getVenueId()).isEmpty()
        || !Set.of("approved", "rejected", "correction_requested").contains(entity.getStatus())
        || entity.getAppealedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Revisión no disponible");
    }
    Map<String, Object> before = snapshot(entity);
    Instant now = clock.instant();
    entity.setStatus("appealed");
    entity.setAppealCode(request.reasonCode());
    entity.setAppealedByUserId(actorId);
    entity.setAppealedAt(now);
    entity.setUpdatedAt(now);
    reviewDao.saveAndFlush(entity);
    audit(
        actorId, "venue_owner", entity, "human_review.appealed", before, snapshot(entity), context);
    return response(entity);
  }

  private DemandHumanReviewResponse create(
      String sourceService, DemandHumanReviewSubmissionRequest request) {
    Instant now = clock.instant();
    DemandHumanReviewEntity entity = new DemandHumanReviewEntity();
    entity.setId(request.reviewId());
    entity.setReviewType(request.reviewType());
    entity.setSubjectType(request.subjectType());
    entity.setSubjectKey(request.subjectKey());
    entity.setSubjectVersion(request.subjectVersion());
    entity.setVenueId(request.venueId());
    entity.setPolicyVersion(request.policyVersion());
    entity.setExplanationCode(request.explanationCode());
    entity.setEvidenceSha256(request.evidenceSha256());
    entity.setStatus("submitted");
    entity.setRequestedByService(sourceService);
    entity.setSubmittedAt(now);
    entity.setUpdatedAt(now);
    reviewDao.saveAndFlush(entity);
    audit(null, "system", entity, "human_review.submitted", null, snapshot(entity), null);
    return response(entity);
  }

  private DemandHumanReviewResponse replay(
      DemandHumanReviewEntity entity, DemandHumanReviewSubmissionRequest request) {
    if (!entity.getReviewType().equals(request.reviewType())
        || !entity.getSubjectType().equals(request.subjectType())
        || !entity.getSubjectKey().equals(request.subjectKey())
        || !entity.getSubjectVersion().equals(request.subjectVersion())
        || !java.util.Objects.equals(entity.getVenueId(), request.venueId())
        || !entity.getPolicyVersion().equals(request.policyVersion())
        || !entity.getExplanationCode().equals(request.explanationCode())
        || !entity.getEvidenceSha256().equals(request.evidenceSha256())) {
      throw conflict();
    }
    return response(entity);
  }

  private void validateSubmission(
      String sourceService, DemandHumanReviewSubmissionRequest request) {
    if (sourceService == null
        || !sourceService.matches("^[a-z][A-Za-z0-9._:-]{0,63}$")
        || request == null
        || !REVIEWABLE_SUBJECTS.contains(request.subjectType())
        || ("attribute".equals(request.reviewType()) && request.venueId() != null)
        || ("commercial_decision".equals(request.reviewType()) && request.venueId() == null)
        || (request.venueId() != null && !venueDao.existsById(request.venueId()))) {
      throw invalid();
    }
  }

  private DemandHumanReviewEntity locked(UUID reviewId) {
    return reviewDao
        .findByIdForUpdate(reviewId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revisión no encontrada"));
  }

  private Map<String, Object> snapshot(DemandHumanReviewEntity entity) {
    Map<String, Object> values = new java.util.LinkedHashMap<>();
    values.put("reviewType", entity.getReviewType());
    values.put("subjectType", entity.getSubjectType());
    values.put("subjectKey", entity.getSubjectKey());
    values.put("subjectVersion", entity.getSubjectVersion());
    values.put("policyVersion", entity.getPolicyVersion());
    values.put("status", entity.getStatus());
    values.put("explanationCode", entity.getExplanationCode());
    if (entity.getVenueId() != null) values.put("venueId", entity.getVenueId());
    if (entity.getReviewReasonCode() != null)
      values.put("reviewReasonCode", entity.getReviewReasonCode());
    if (entity.getCorrectionVersion() != null)
      values.put("correctionVersion", entity.getCorrectionVersion());
    if (entity.getAppealCode() != null) values.put("appealCode", entity.getAppealCode());
    return Map.copyOf(values);
  }

  private void audit(
      UUID actorId,
      String role,
      DemandHumanReviewEntity entity,
      String action,
      Map<String, Object> before,
      Map<String, Object> after,
      AdminRequestContext context) {
    auditLogService.record(
        new AuditLogEntry(
            actorId,
            role,
            "demand_human_review",
            entity.getId(),
            action,
            before,
            after,
            context == null ? null : context.ipAddress(),
            context == null ? null : context.userAgent()));
  }

  private DemandHumanReviewResponse response(DemandHumanReviewEntity entity) {
    return new DemandHumanReviewResponse(
        entity.getId(),
        entity.getReviewType(),
        entity.getSubjectType(),
        entity.getSubjectKey(),
        entity.getSubjectVersion(),
        entity.getVenueId(),
        entity.getPolicyVersion(),
        entity.getExplanationCode(),
        entity.getStatus(),
        entity.getReviewReasonCode(),
        entity.getCorrectionVersion(),
        entity.getAppealCode(),
        "approved".equals(entity.getStatus()),
        entity.getSubmittedAt(),
        entity.getUpdatedAt());
  }

  private ResponseStatusException invalid() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisión inválida");
  }

  private ResponseStatusException conflict() {
    return new ResponseStatusException(HttpStatus.CONFLICT, "Transición o replay inválido");
  }
}
