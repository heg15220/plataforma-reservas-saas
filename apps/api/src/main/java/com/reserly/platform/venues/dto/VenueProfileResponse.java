package com.reserly.platform.venues.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Vista privada del perfil sin IDs de propietario, cuenta empresarial ni datos fiscales. */
public record VenueProfileResponse(
    UUID id,
    UUID categoryId,
    String categorySlug,
    String categoryName,
    String name,
    String slug,
    String description,
    LocalizedTextDto descriptionI18n,
    LocalizedTextDto servicesI18n,
    LocalizedTextDto rulesI18n,
    LocalizedTextDto publicTextI18n,
    String defaultLocale,
    String contactEmail,
    String phone,
    String address,
    String city,
    String province,
    String country,
    String postalCode,
    BigDecimal latitude,
    BigDecimal longitude,
    String mainImageUrl,
    String status,
    boolean showPhone,
    boolean showEmail,
    Instant createdAt,
    Instant updatedAt) {}
