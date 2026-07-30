package com.reserly.platform.administration.dto;

import java.util.List;

/** Catálogo administrativo completo en orden estable. */
public record AdminCategoryListResponse(List<AdminCategoryResponse> categories) {
  public AdminCategoryListResponse {
    categories = List.copyOf(categories);
  }
}
