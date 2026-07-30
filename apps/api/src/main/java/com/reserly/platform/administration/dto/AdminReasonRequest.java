package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo administrativo común para acciones cuyo resultado lo fija la ruta. */
public record AdminReasonRequest(@NotBlank @Size(max = 1000) String reason) {}
