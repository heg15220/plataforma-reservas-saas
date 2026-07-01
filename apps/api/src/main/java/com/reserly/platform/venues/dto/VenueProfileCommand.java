package com.reserly.platform.venues.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Comando interno del snapshot editable, separado del contrato HTTP. */
public record VenueProfileCommand(
    String name,
    UUID categoryId,
    String description,
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
    boolean showPhone,
    boolean showEmail) {}
