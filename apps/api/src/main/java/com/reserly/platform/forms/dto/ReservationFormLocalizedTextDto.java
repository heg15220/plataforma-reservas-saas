package com.reserly.platform.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** Texto p?blico localizado de un label u opci?n del formulario. */
public record ReservationFormLocalizedTextDto(
    @NotNull @Pattern(regexp = "es|en") String sourceLocale,
    @NotNull @Size(min = 1, max = 2)
        Map<@Pattern(regexp = "es|en") String, @NotBlank @Size(max = 160) String> values) {}
