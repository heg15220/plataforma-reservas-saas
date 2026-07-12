package com.reserly.platform.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload privado para crear o editar un empleado, profesional o recurso reservable. */
public record EmployeeResourceRequest(
    @NotBlank @Pattern(regexp = "employee|professional|room|court|table|equipment|other")
        String type,
    @Size(max = 120) String firstName,
    @Size(max = 160) String lastName,
    @Size(max = 160) String publicAlias,
    @Size(max = 2048) String photoUrl,
    @Size(max = 240) String specialty,
    @Size(max = 2000) String description,
    @NotBlank @Pattern(regexp = "active|inactive|internal_only|archived") String status,
    boolean publicVisibility,
    @Size(max = 2000) String internalNotes) {}
