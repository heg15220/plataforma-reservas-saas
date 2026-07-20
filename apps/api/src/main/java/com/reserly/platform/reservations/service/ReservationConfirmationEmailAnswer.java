package com.reserly.platform.reservations.service;

/** Snapshot mínimo y serializable de respuesta que necesitarán las plantillas de confirmación. */
public record ReservationConfirmationEmailAnswer(String key, String label, String valueJson) {}
