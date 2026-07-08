package com.reserly.platform.availability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Reemplazo completo del horario semanal del local autenticado. */
public record OpeningHoursUpdateRequest(@Valid @NotEmpty List<OpeningHourRequest> days) {}
