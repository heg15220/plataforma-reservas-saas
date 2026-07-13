package com.reserly.platform.forms.dto;

/** Cambio editorial del formulario; el fallback exige aprobaci?n expl?cita en cada publicaci?n. */
public record ReservationFormPublicationRequest(boolean published, boolean fallbackApproved) {}
