package com.reserly.platform.forms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Payload privado con textos localizados y todos los atributos editables del campo. */
public record ReservationFormFieldRequest(
    @NotNull @Valid ReservationFormLocalizedTextDto labelI18n,
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$") String key,
    @NotBlank String type,
    boolean required,
    @Size(max = 50) List<@NotNull @Valid ReservationFormLocalizedTextDto> optionsI18n) {}
