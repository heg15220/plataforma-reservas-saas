package com.reserly.platform.forms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Orden completo de los campos activos. Debe incluir una vez cada campo propio para evitar
 * actualizaciones parciales ambiguas.
 */
public record ReservationFormFieldOrderRequest(
    @NotNull List<@NotNull UUID> fieldIds) {}
