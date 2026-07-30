package com.reserly.platform.administration.dto;

import java.time.Instant;
import java.util.UUID;

/** Datos fiscales mínimos necesarios para revisar una cuenta empresarial pendiente. */
public record AdminBusinessAccountResponse(
    UUID id, UUID ownerUserId, String ownerEmail, String taxCountry,
    String businessLegalName, String businessTaxIdentifier, String businessAddress,
    String verificationStatus, String verificationProvider, String verificationReference,
    String manualReviewStatus, Instant updatedAt) {}
