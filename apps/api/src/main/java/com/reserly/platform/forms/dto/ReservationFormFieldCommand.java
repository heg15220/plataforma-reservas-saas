package com.reserly.platform.forms.dto;

import com.reserly.platform.localization.LocalizedText;
import java.util.List;

/** Comando localizado de aplicaci?n independiente del transporte HTTP. */
public record ReservationFormFieldCommand(
    LocalizedText labelI18n,
    String key,
    String type,
    boolean required,
    List<LocalizedText> optionsI18n) {}
