package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminCategoryListResponse;
import com.reserly.platform.administration.dto.AdminCategoryRequest;
import com.reserly.platform.administration.dto.AdminCategoryResponse;
import java.util.UUID;

/** Gestión transaccional del catálogo global de categorías. */
public interface AdminCategoryService {
  AdminCategoryListResponse list();

  AdminCategoryResponse create(
      UUID actorUserId, AdminCategoryRequest request, AdminRequestContext context);

  AdminCategoryResponse update(
      UUID actorUserId, UUID categoryId, AdminCategoryRequest request, AdminRequestContext context);
}
