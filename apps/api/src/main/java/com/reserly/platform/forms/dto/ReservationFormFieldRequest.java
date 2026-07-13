package com.reserly.platform.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload privado de los atributos editables en las tareas 6.3 y 6.4. */
public record ReservationFormFieldRequest(
    @NotBlank @Size(max = 160) String label,
    @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$")
        String key,
    @NotBlank String type) {}
