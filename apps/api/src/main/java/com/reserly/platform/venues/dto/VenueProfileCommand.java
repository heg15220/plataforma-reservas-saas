package com.reserly.platform.venues.dto;

import com.reserly.platform.localization.LocalizedText;
import java.math.BigDecimal;
import java.util.UUID;

/** Comando interno del snapshot editable, separado del contrato HTTP. */
public record VenueProfileCommand(
    String name,
    UUID categoryId,
    LocalizedText descriptionI18n,
    LocalizedText servicesI18n,
    LocalizedText rulesI18n,
    LocalizedText publicTextI18n,
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
