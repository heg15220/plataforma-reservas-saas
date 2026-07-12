package com.reserly.platform.resources.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Reemplazo completo del horario semanal de un recurso. */
public record EmployeeResourceWeeklyHoursRequest(
    @NotNull @Size(max = 7) List<@Valid EmployeeResourceHourRequest> hours) {}
