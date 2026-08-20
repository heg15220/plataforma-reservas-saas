package com.reserly.platform.demand.experiment;

import java.time.Instant;
import java.util.UUID;

/**
 * Vincula una asignación a la decisión que va a exponerse.
 *
 * @param assignmentId asignación durable obtenida antes del ranking
 * @param recommendationRequestId identificador público de la decisión persistida
 * @param exposedAt instante anterior o igual al registro de impresión del cliente
 */
public record ExperimentExposureCommand(
    UUID assignmentId, UUID recommendationRequestId, Instant exposedAt) {}
