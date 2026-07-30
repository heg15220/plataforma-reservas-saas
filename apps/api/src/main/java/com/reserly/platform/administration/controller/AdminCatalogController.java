package com.reserly.platform.administration.controller;

import com.reserly.platform.administration.dto.AdminAuditLogListResponse;
import com.reserly.platform.administration.dto.AdminBusinessAccountListResponse;
import com.reserly.platform.administration.dto.AdminBusinessAccountResponse;
import com.reserly.platform.administration.dto.AdminBusinessRecheckRequest;
import com.reserly.platform.administration.dto.AdminCategoryListResponse;
import com.reserly.platform.administration.dto.AdminCategoryRequest;
import com.reserly.platform.administration.dto.AdminCategoryResponse;
import com.reserly.platform.administration.dto.AdminDocumentListResponse;
import com.reserly.platform.administration.dto.AdminDocumentResponse;
import com.reserly.platform.administration.dto.AdminDocumentReviewRequest;
import com.reserly.platform.administration.dto.AdminIncidentListResponse;
import com.reserly.platform.administration.dto.AdminIncidentResponse;
import com.reserly.platform.administration.dto.AdminIncidentReviewRequest;
import com.reserly.platform.administration.dto.AdminMetricsResponse;
import com.reserly.platform.administration.dto.AdminPenaltyListResponse;
import com.reserly.platform.administration.dto.AdminPenaltyResponse;
import com.reserly.platform.administration.dto.AdminPenaltyUpdateRequest;
import com.reserly.platform.administration.dto.AdminPlanListResponse;
import com.reserly.platform.administration.dto.AdminPlanRequest;
import com.reserly.platform.administration.dto.AdminPlanResponse;
import com.reserly.platform.administration.dto.AdminReasonRequest;
import com.reserly.platform.administration.dto.AdminVenueListResponse;
import com.reserly.platform.administration.dto.AdminVenueResponse;
import com.reserly.platform.administration.dto.AdminVenueSuspensionRequest;
import com.reserly.platform.administration.dto.AdminVenueUpdateRequest;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato administrativo inicial protegido globalmente por {@code ROLE_ADMIN}. */
@RequestMapping(path = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public interface AdminCatalogController {

  @GetMapping("/categories")
  ResponseEntity<AdminCategoryListResponse> listCategories();

  @PostMapping(path = "/categories", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminCategoryResponse> createCategory(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody AdminCategoryRequest request,
      HttpServletRequest servletRequest);

  @PatchMapping(path = "/categories/{categoryId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminCategoryResponse> updateCategory(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID categoryId,
      @Valid @RequestBody AdminCategoryRequest request,
      HttpServletRequest servletRequest);

  @GetMapping("/venues")
  ResponseEntity<AdminVenueListResponse> listVenues();

  @PatchMapping(path = "/venues/{venueId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminVenueResponse> updateVenue(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID venueId,
      @Valid @RequestBody AdminVenueUpdateRequest request,
      HttpServletRequest servletRequest);

  @PatchMapping(path = "/venues/{venueId}/suspension", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminVenueResponse> suspendVenue(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID venueId,
      @Valid @RequestBody AdminVenueSuspensionRequest request,
      HttpServletRequest servletRequest);

  @GetMapping("/incidents")
  ResponseEntity<AdminIncidentListResponse> listIncidents();

  @PatchMapping(path = "/incidents/{incidentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminIncidentResponse> reviewIncident(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID incidentId,
      @Valid @RequestBody AdminIncidentReviewRequest request,
      HttpServletRequest servletRequest);

  @GetMapping("/business-accounts")
  ResponseEntity<AdminBusinessAccountListResponse> listPendingBusinessAccounts();

  @GetMapping("/business-accounts/{accountId}")
  ResponseEntity<AdminBusinessAccountResponse> getPendingBusinessAccount(
      @PathVariable UUID accountId);

  @PostMapping(
      path = "/business-accounts/{accountId}/approve",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminBusinessAccountResponse> approveBusinessAccount(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID accountId,
      @Valid @RequestBody AdminReasonRequest request,
      HttpServletRequest servletRequest);

  @PostMapping(
      path = "/business-accounts/{accountId}/reject",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminBusinessAccountResponse> rejectBusinessAccount(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID accountId,
      @Valid @RequestBody AdminReasonRequest request,
      HttpServletRequest servletRequest);

  @PostMapping(
      path = "/business-accounts/{accountId}/recheck",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminBusinessAccountResponse> recheckBusinessAccount(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID accountId,
      @Valid @RequestBody AdminBusinessRecheckRequest request,
      HttpServletRequest servletRequest);

  @GetMapping("/business-documents")
  ResponseEntity<AdminDocumentListResponse> listPendingDocuments();

  @GetMapping(
      path = "/business-documents/{documentId}/content",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  ResponseEntity<byte[]> getDocumentContent(@PathVariable UUID documentId);

  @PatchMapping(
      path = "/business-documents/{documentId}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminDocumentResponse> reviewDocument(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID documentId,
      @Valid @RequestBody AdminDocumentReviewRequest request,
      HttpServletRequest servletRequest);

  @GetMapping("/penalties")
  ResponseEntity<AdminPenaltyListResponse> listPenalties();

  @PatchMapping(path = "/penalties/{penaltyId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminPenaltyResponse> updatePenalty(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID penaltyId,
      @Valid @RequestBody AdminPenaltyUpdateRequest request,
      HttpServletRequest servletRequest);

  @GetMapping("/plans")
  ResponseEntity<AdminPlanListResponse> listPlans();

  @PostMapping(path = "/plans", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminPlanResponse> createPlan(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody AdminPlanRequest request,
      HttpServletRequest servletRequest);

  @PatchMapping(path = "/plans/{planId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AdminPlanResponse> updatePlan(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID planId,
      @Valid @RequestBody AdminPlanRequest request,
      HttpServletRequest servletRequest);

  @GetMapping("/metrics")
  ResponseEntity<AdminMetricsResponse> getMetrics();

  @GetMapping("/audit-logs")
  ResponseEntity<AdminAuditLogListResponse> listAuditLogs();
}
