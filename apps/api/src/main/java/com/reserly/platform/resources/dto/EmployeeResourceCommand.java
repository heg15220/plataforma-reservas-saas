package com.reserly.platform.resources.dto;

/** Comando interno del caso de uso de equipo; la propiedad se toma de la sesión. */
public record EmployeeResourceCommand(
    String type,
    String firstName,
    String lastName,
    String publicAlias,
    String photoUrl,
    String specialty,
    String description,
    String status,
    boolean publicVisibility,
    String internalNotes) {}
