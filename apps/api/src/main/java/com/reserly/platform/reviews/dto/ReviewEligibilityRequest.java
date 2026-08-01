package com.reserly.platform.reviews.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Email mínimo usado para comprobar elegibilidad sin devolver historial de reservas. */
public record ReviewEligibilityRequest(@NotBlank @Email @Size(max = 320) String customerEmail) {}
