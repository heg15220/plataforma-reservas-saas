package com.reserly.platform.demand.experiment;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicita una variante estable para una unidad seudónima.
 *
 * @param experimentKey experimento lógico activo
 * @param assignmentUnitId sesión o identidad seudónima estable
 * @param assignedAt instante de decisión, usado también para resolver la ventana activa
 */
public record ExperimentAssignmentCommand(
    String experimentKey, UUID assignmentUnitId, Instant assignedAt) {}
