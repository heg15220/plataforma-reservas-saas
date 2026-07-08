package com.reserly.platform.availability.dto;

import java.util.List;

/** Snapshot ordenado del horario semanal. */
public record OpeningHoursResponse(List<OpeningHourResponse> days) {}
