package com.reserly.platform.forms.converter;

import com.reserly.platform.forms.dto.ReservationFormFieldCommand;
import com.reserly.platform.forms.dto.ReservationFormFieldRequest;
import com.reserly.platform.forms.dto.ReservationFormFieldResponse;
import com.reserly.platform.forms.dto.ReservationFormLocalizedTextDto;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.service.ReservationFormFieldInvalidException;
import com.reserly.platform.localization.LocalizedText;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Convierte contratos REST localizados sin tomar decisiones de propiedad o persistencia. */
@Component
public class ReservationFormFieldConverter {

  public ReservationFormFieldCommand toCommand(ReservationFormFieldRequest request) {
    return new ReservationFormFieldCommand(
        toLocalizedText(request.labelI18n()),
        request.key(),
        request.type(),
        request.required(),
        toLocalizedTexts(request.optionsI18n()));
  }

  public ReservationFormFieldResponse toResponse(ReservationFormFieldEntity field) {
    return new ReservationFormFieldResponse(
        field.getId(),
        field.getLabel(),
        toDto(field.getLabelI18n()),
        field.getKey(),
        field.getType().code(),
        field.isRequired(),
        copy(field.getOptions()),
        toDtos(field.getOptionsI18n()),
        field.getPosition(),
        field.isActive(),
        field.getCreatedAt(),
        field.getUpdatedAt());
  }

  public ReservationFormLocalizedTextDto toDto(LocalizedText value) {
    if (value == null) {
      return null;
    }
    return new ReservationFormLocalizedTextDto(
        value.sourceLocale().languageTag(), value.toLanguageTagValues());
  }

  public List<ReservationFormLocalizedTextDto> toDtos(List<LocalizedText> values) {
    return values == null ? null : values.stream().map(this::toDto).toList();
  }

  private LocalizedText toLocalizedText(ReservationFormLocalizedTextDto value) {
    if (value == null || hasUnsupportedLocale(value.values())) {
      throw new ReservationFormFieldInvalidException();
    }
    try {
      return LocalizedText.fromLanguageTagValues(value.sourceLocale(), value.values());
    } catch (IllegalArgumentException exception) {
      throw new ReservationFormFieldInvalidException(exception);
    }
  }

  private List<LocalizedText> toLocalizedTexts(List<ReservationFormLocalizedTextDto> values) {
    return values == null ? null : values.stream().map(this::toLocalizedText).toList();
  }

  private boolean hasUnsupportedLocale(Map<String, String> values) {
    return values == null
        || values.keySet().stream().anyMatch(key -> !key.equals("es") && !key.equals("en"));
  }

  private List<String> copy(List<String> values) {
    return values == null ? null : List.copyOf(values);
  }
}
