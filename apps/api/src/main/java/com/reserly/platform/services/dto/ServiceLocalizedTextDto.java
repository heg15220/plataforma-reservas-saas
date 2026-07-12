package com.reserly.platform.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** Texto localizado opcional para nombre o descripcion de un servicio. */
public record ServiceLocalizedTextDto(
    @NotNull @Pattern(regexp = "es|en") String sourceLocale,
    @NotNull @Size(min = 1, max = 2)
        Map<@Pattern(regexp = "es|en") String, @NotBlank @Size(max = 2000) String> values) {}
