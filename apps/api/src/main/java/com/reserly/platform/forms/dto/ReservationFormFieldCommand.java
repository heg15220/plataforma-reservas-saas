package com.reserly.platform.forms.dto;

/** Comando de aplicacion independiente del transporte HTTP. */
public record ReservationFormFieldCommand(String label, String key, String type) {}
