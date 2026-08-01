package com.reserly.platform.forms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.reserly.platform.forms.converter.ReservationFormFieldConverter;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica campos base y personalizados localizados en la previsualización. */
@ExtendWith(MockitoExtension.class)
class ReservationFormPreviewServiceTests {

  @Mock private ReservationFormFieldService fieldService;

  private ReservationFormPreviewServiceImpl previewService;
  private UUID ownerId;

  @BeforeEach
  void setUp() {
    previewService =
        new ReservationFormPreviewServiceImpl(fieldService, new ReservationFormFieldConverter());
    ownerId = UUID.randomUUID();
  }

  @Test
  void placesImmutableBaseFieldsBeforeLocalizedCustomFields() {
    ReservationFormFieldEntity allergies =
        customField("allergies", "Alergias", ReservationFormFieldType.LONG_TEXT, true, null);
    ReservationFormFieldEntity area =
        customField(
            "area", "Zona", ReservationFormFieldType.SELECT, false, List.of("Interior", "Terraza"));
    when(fieldService.list(ownerId)).thenReturn(List.of(allergies, area));

    ReservationFormPreviewResponse preview = previewService.preview(ownerId);

    assertThat(preview.fields()).hasSize(7);
    assertThat(preview.fields().subList(0, 5))
        .extracting(field -> field.key())
        .containsExactly(
            "customer_name", "customer_email", "party_size", "reservation_date", "time_slot");
    assertThat(preview.fields().subList(0, 5))
        .allSatisfy(
            field -> {
              assertThat(field.source()).isEqualTo("base");
              assertThat(field.id()).isNull();
              assertThat(field.required()).isTrue();
              assertThat(field.editable()).isFalse();
              assertThat(field.label()).isNull();
              assertThat(field.labelKey()).isNotBlank();
              assertThat(field.labelI18n()).isNull();
            });

    var customFields = preview.fields().subList(5, 7);
    assertThat(customFields).extracting(field -> field.key()).containsExactly("allergies", "area");
    assertThat(customFields).extracting(field -> field.position()).containsExactly(5, 6);
    assertThat(customFields.get(0).labelI18n().values())
        .containsEntry("es", "Alergias")
        .containsEntry("en", "Alergias EN");
    assertThat(customFields.get(1).optionsI18n()).hasSize(2);
    assertThat(customFields.get(1).options()).containsExactly("Interior", "Terraza");
  }

  @Test
  void propagatesMissingOwnedVenueWithoutProducingPartialPreview() {
    when(fieldService.list(ownerId)).thenThrow(new ReservationFormFieldNotFoundException());

    assertThatThrownBy(() -> previewService.preview(ownerId))
        .isInstanceOf(ReservationFormFieldNotFoundException.class);
  }

  private ReservationFormFieldEntity customField(
      String key,
      String label,
      ReservationFormFieldType type,
      boolean required,
      List<String> options) {
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setId(UUID.randomUUID());
    field.setKey(key);
    field.setLabel(label);
    field.setLabelI18n(localized(label));
    field.setType(type);
    field.setRequired(required);
    field.setOptions(options);
    field.setOptionsI18n(options == null ? null : options.stream().map(this::localized).toList());
    field.setActive(true);
    return field;
  }

  private LocalizedText localized(String value) {
    return new LocalizedText(
        SupportedLocale.ES, Map.of(SupportedLocale.ES, value, SupportedLocale.EN, value + " EN"));
  }
}
