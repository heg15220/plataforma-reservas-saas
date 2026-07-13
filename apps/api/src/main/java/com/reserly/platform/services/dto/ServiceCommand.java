package com.reserly.platform.services.dto;

import com.reserly.platform.localization.LocalizedText;

/** Comando interno del caso de uso; el local propietario se deriva del principal autenticado. */
public record ServiceCommand(
    String name,
    LocalizedText nameI18n,
    String description,
    LocalizedText descriptionI18n,
    int durationMinutes,
    int capacityRequired,
    boolean active,
    Boolean allowsAnyAvailableResource) {}
