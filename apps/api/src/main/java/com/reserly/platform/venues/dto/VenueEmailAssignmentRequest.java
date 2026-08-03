package com.reserly.platform.venues.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Email operativo y contraseña de acceso elegidos para el panel de un local concreto. */
public record VenueEmailAssignmentRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(min = 12, max = 72) String password) {}
