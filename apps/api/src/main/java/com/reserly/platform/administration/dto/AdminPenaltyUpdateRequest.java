package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Revocación o ajuste de vigencia de una penalización activa. */
public record AdminPenaltyUpdateRequest(
    @NotBlank @Pattern(regexp = "active|revoked") String status,
    @Future Instant endsAt,
    @NotBlank @Size(max = 500) String reason) {}
