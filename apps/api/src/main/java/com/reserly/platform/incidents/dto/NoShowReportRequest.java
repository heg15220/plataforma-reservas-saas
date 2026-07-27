package com.reserly.platform.incidents.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Confirmación explícita del reporte de no asistencia.
 *
 * @param confirmed debe ser {@code true}; evita ejecutar la acción crítica por una pulsación
 *     ambigua
 * @param notes contexto profesional opcional, almacenado como texto plano
 */
public record NoShowReportRequest(
    @AssertTrue boolean confirmed, @Size(max = 2000) String notes) {}
