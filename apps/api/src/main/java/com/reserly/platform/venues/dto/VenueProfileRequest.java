package com.reserly.platform.venues.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Snapshot editable del perfil.
 *
 * <p>Se usa tanto para alta como para PATCH sustitutivo: los campos opcionales con {@code null} se
 * eliminan. No admite estado, slug, propiedad, imagen ni publicación.
 */
public record VenueProfileRequest(
    @NotBlank @Size(max = 160) String name,
    @NotNull UUID categoryId,
    @Size(max = 10_000) String description,
    @NotNull @Pattern(regexp = "es|en") String defaultLocale,
    @Email @Size(max = 320) String contactEmail,
    @Size(max = 32) String phone,
    @Size(max = 500) String address,
    @Size(max = 160) String city,
    @Size(max = 160) String province,
    @Pattern(regexp = "[A-Z]{2}") String country,
    @Size(max = 24) String postalCode,
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    boolean showPhone,
    boolean showEmail) {}
