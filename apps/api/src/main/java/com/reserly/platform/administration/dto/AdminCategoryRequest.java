package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos administrables de una categoría con traducciones obligatorias ES/EN. */
public record AdminCategoryRequest(
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9]*(?:-[a-z0-9]+)*") @Size(max = 120) String slug,
    @NotBlank @Size(max = 120) String nameEs,
    @NotBlank @Size(max = 120) String nameEn,
    boolean active) {}
