package com.reserly.platform.reservations.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Contrato público para convertir el hold anónimo en una reserva identificada. */
public record ReservationConfirmRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String holdToken,
    @NotBlank @Size(max = 160) String customerName,
    @NotBlank @Email @Size(max = 320) String customerEmail,
    @NotBlank @Pattern(regexp = "^(es|en)$") String locale,
    @Min(1) int partySize,
    @NotNull @Size(max = 100) List<@Valid ReservationConfirmFormResponse> formResponses,
    @AssertTrue boolean acceptsPrivacyPolicy,
    @AssertTrue boolean acceptsBookingRules) {}
