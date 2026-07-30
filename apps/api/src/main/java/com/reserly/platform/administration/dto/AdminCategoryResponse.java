package com.reserly.platform.administration.dto;

import java.time.Instant;
import java.util.UUID;

/** Categoría administrativa sin exponer el documento JSONB interno. */
public record AdminCategoryResponse(
    UUID id,
    String slug,
    String nameEs,
    String nameEn,
    boolean active,
    Instant updatedAt) {}
