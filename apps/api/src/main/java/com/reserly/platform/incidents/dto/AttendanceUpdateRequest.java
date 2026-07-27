package com.reserly.platform.incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Transición explícita de asistencia.
 *
 * @param status uno de {@code attended}, {@code no_show} o {@code pending}
 */
public record AttendanceUpdateRequest(@NotBlank @Size(max = 16) String status) {}
