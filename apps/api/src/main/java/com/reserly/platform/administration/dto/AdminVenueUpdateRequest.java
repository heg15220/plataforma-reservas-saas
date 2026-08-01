package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Edición administrativa básica; excluye estado, propiedad, slug y contenido editorial. */
public record AdminVenueUpdateRequest(
    @NotBlank @Size(max = 160) String name,
    @NotNull UUID categoryId,
    @Email @Size(max = 320) String contactEmail,
    @Size(max = 32) String phone,
    @Size(max = 500) String address,
    @Size(max = 160) String city,
    @Size(max = 160) String province,
    @Pattern(regexp = "[A-Z]{2}") String country,
    @Size(max = 24) String postalCode) {}
