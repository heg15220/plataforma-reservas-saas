package com.reserly.platform.venues.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Email operativo elegido por el propietario para un local publicado concreto. */
public record VenueEmailAssignmentRequest(@NotBlank @Email @Size(max = 320) String email) {}
