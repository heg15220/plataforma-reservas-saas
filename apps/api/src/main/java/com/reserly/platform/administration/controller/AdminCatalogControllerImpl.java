package com.reserly.platform.administration.controller;

import com.reserly.platform.administration.dto.AdminCategoryListResponse;
import com.reserly.platform.administration.dto.AdminCategoryRequest;
import com.reserly.platform.administration.dto.AdminCategoryResponse;
import com.reserly.platform.administration.dto.AdminVenueListResponse;
import com.reserly.platform.administration.dto.AdminVenueResponse;
import com.reserly.platform.administration.dto.AdminVenueUpdateRequest;
import com.reserly.platform.administration.dto.AdminVenueSuspensionRequest;
import com.reserly.platform.administration.dto.AdminIncidentListResponse;
import com.reserly.platform.administration.dto.AdminIncidentResponse;
import com.reserly.platform.administration.dto.AdminIncidentReviewRequest;
import com.reserly.platform.administration.dto.AdminBusinessAccountListResponse;
import com.reserly.platform.administration.dto.AdminBusinessAccountResponse;
import com.reserly.platform.administration.service.AdminBusinessAccountService;
import com.reserly.platform.administration.service.AdminCategoryService;
import com.reserly.platform.administration.service.AdminIncidentService;
import com.reserly.platform.administration.service.AdminRequestContext;
import com.reserly.platform.administration.service.AdminVenueService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adapta principal y metadatos observados sin aceptar actor desde el cuerpo. */
@RestController
public class AdminCatalogControllerImpl implements AdminCatalogController {

  private final AdminCategoryService categoryService;
  private final AdminVenueService venueService;
  private final AdminIncidentService incidentService;
  private final AdminBusinessAccountService businessAccountService;

  public AdminCatalogControllerImpl(
      AdminCategoryService categoryService,
      AdminVenueService venueService,
      AdminIncidentService incidentService,
      AdminBusinessAccountService businessAccountService) {
    this.categoryService = categoryService;
    this.venueService = venueService;
    this.incidentService = incidentService;
    this.businessAccountService = businessAccountService;
  }

  @Override
  public ResponseEntity<AdminCategoryListResponse> listCategories() {
    return ResponseEntity.ok(categoryService.list());
  }

  @Override
  public ResponseEntity<AdminCategoryResponse> createCategory(
      AuthenticatedAccount account,
      AdminCategoryRequest request,
      HttpServletRequest servletRequest) {
    return ResponseEntity.ok(
        categoryService.create(account.userId(), request, context(servletRequest)));
  }

  @Override
  public ResponseEntity<AdminCategoryResponse> updateCategory(
      AuthenticatedAccount account,
      UUID categoryId,
      AdminCategoryRequest request,
      HttpServletRequest servletRequest) {
    return ResponseEntity.ok(
        categoryService.update(account.userId(), categoryId, request, context(servletRequest)));
  }

  @Override
  public ResponseEntity<AdminVenueListResponse> listVenues() {
    return ResponseEntity.ok(venueService.list());
  }

  @Override
  public ResponseEntity<AdminVenueResponse> updateVenue(
      AuthenticatedAccount account,
      UUID venueId,
      AdminVenueUpdateRequest request,
      HttpServletRequest servletRequest) {
    return ResponseEntity.ok(
        venueService.update(account.userId(), venueId, request, context(servletRequest)));
  }

  @Override
  public ResponseEntity<AdminVenueResponse> suspendVenue(
      AuthenticatedAccount account,
      UUID venueId,
      AdminVenueSuspensionRequest request,
      HttpServletRequest servletRequest) {
    return ResponseEntity.ok(
        venueService.suspend(account.userId(), venueId, request, context(servletRequest)));
  }

  @Override
  public ResponseEntity<AdminIncidentListResponse> listIncidents() {
    return ResponseEntity.ok(incidentService.list());
  }

  @Override
  public ResponseEntity<AdminIncidentResponse> reviewIncident(
      AuthenticatedAccount account,
      UUID incidentId,
      AdminIncidentReviewRequest request,
      HttpServletRequest servletRequest) {
    return ResponseEntity.ok(
        incidentService.review(account.userId(), incidentId, request, context(servletRequest)));
  }

  @Override
  public ResponseEntity<AdminBusinessAccountListResponse> listPendingBusinessAccounts() {
    return ResponseEntity.ok(businessAccountService.listPending());
  }

  @Override
  public ResponseEntity<AdminBusinessAccountResponse> getPendingBusinessAccount(UUID accountId) {
    return ResponseEntity.ok(businessAccountService.getPending(accountId));
  }

  private AdminRequestContext context(HttpServletRequest request) {
    return new AdminRequestContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
  }
}
