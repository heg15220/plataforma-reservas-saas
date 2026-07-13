package com.reserly.platform.forms.dto;

import java.util.List;

/** Comando de aplicación independiente del transporte HTTP. */
public record ReservationFormFieldCommand(
    String label, String key, String type, boolean required, List<String> options) {}
