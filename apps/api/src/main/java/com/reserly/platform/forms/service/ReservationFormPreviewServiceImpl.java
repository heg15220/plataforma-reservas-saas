package com.reserly.platform.forms.service;

import com.reserly.platform.forms.ReservationBaseFieldCatalog;
import com.reserly.platform.forms.ReservationBaseFieldDefinition;
import com.reserly.platform.forms.dto.ReservationFormPreviewFieldResponse;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Implementación que antepone el catálogo base inmutable al orden custom persistido. */
@Service
public class ReservationFormPreviewServiceImpl implements ReservationFormPreviewService {
  private static final String BASE_SOURCE = "base";
  private static final String CUSTOM_SOURCE = "custom";

  private final ReservationFormFieldService fieldService;

  public ReservationFormPreviewServiceImpl(ReservationFormFieldService fieldService) {
    this.fieldService = fieldService;
  }

  @Override
  public ReservationFormPreviewResponse preview(UUID ownerUserId) {
    List<ReservationBaseFieldDefinition> baseFields = ReservationBaseFieldCatalog.fields();
    List<ReservationFormFieldEntity> customFields = fieldService.list(ownerUserId);
    List<ReservationFormPreviewFieldResponse> preview =
        new ArrayList<>(baseFields.size() + customFields.size());

    baseFields.stream().map(this::toBasePreview).forEach(preview::add);
    for (int index = 0; index < customFields.size(); index++) {
      preview.add(toCustomPreview(customFields.get(index), baseFields.size() + index));
    }
    return new ReservationFormPreviewResponse(preview);
  }

  private ReservationFormPreviewFieldResponse toBasePreview(
      ReservationBaseFieldDefinition field) {
    return new ReservationFormPreviewFieldResponse(
        null,
        BASE_SOURCE,
        field.key(),
        field.inputType(),
        null,
        field.labelKey(),
        field.required(),
        field.editable(),
        null,
        field.position());
  }

  private ReservationFormPreviewFieldResponse toCustomPreview(
      ReservationFormFieldEntity field, int previewPosition) {
    return new ReservationFormPreviewFieldResponse(
        field.getId(),
        CUSTOM_SOURCE,
        field.getKey(),
        field.getType().code(),
        field.getLabel(),
        null,
        field.isRequired(),
        true,
        field.getOptions(),
        previewPosition);
  }
}
