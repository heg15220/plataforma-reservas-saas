package com.reserly.platform.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Payload privado con todos los atributos editables de un campo personalizado. */
public record ReservationFormFieldRequest(
    @NotBlank @Size(max = 160) String label,
    @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$")
        String key,
    @NotBlank String type,
    boolean required,
    @Size(max = 50) List<@NotBlank @Size(max = 160) String> options) {}
