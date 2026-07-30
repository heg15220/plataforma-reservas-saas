package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Orden idempotente de revalidación remota iniciada por un administrador. */
public record AdminBusinessRecheckRequest(
    @NotNull UUID requestId,
    @Size(max = 64) String preferredProvider,
    @NotBlank @Size(max = 1000) String reason) {}
