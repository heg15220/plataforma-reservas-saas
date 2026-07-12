package com.reserly.platform.resources.dto;

import java.time.Instant;
import java.util.UUID;

/** Proyección privada de un miembro de equipo o recurso sin exponer el local propietario. */
public record EmployeeResourceResponse(
    UUID id,
    String type,
    String firstName,
    String lastName,
    String publicAlias,
    String photoUrl,
    String specialty,
    String description,
    String status,
    boolean publicVisibility,
    String internalNotes,
    Instant createdAt,
    Instant updatedAt) {}
