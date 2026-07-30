package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo obligatorio y persistido en auditoría para suspender un local. */
public record AdminVenueSuspensionRequest(@NotBlank @Size(max = 500) String reason) {}
