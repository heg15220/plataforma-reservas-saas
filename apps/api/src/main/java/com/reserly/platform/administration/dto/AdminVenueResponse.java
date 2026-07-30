package com.reserly.platform.administration.dto;

import java.time.Instant;
import java.util.UUID;

/** Proyección administrativa minimizada del local. */
public record AdminVenueResponse(
    UUID id,
    String name,
    String slug,
    UUID categoryId,
    String categoryName,
    String status,
    String contactEmail,
    String phone,
    String address,
    String city,
    String province,
    String country,
    String postalCode,
    Instant updatedAt) {}
