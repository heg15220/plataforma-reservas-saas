package com.reserly.platform.administration.controller;

import com.reserly.platform.administration.dto.AdminCategoryListResponse;
import com.reserly.platform.administration.dto.AdminCategoryRequest;
import com.reserly.platform.administration.dto.AdminCategoryResponse;
import com.reserly.platform.administration.dto.AdminVenueListResponse;
import com.reserly.platform.administration.dto.AdminVenueResponse;
import com.reserly.platform.administration.dto.AdminVenueUpdateRequest;
import com.reserly.platform.administration.service.AdminCategoryService;
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

  public AdminCatalogControllerImpl(
      AdminCategoryService categoryService, AdminVenueService venueService) {
    this.categoryService = categoryService;
    this.venueService = venueService;
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

  private AdminRequestContext context(HttpServletRequest request) {
    return new AdminRequestContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
  }
}
