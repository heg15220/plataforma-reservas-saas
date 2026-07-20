package com.reserly.platform.forms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.TextNode;
import com.reserly.platform.forms.dto.ReservationFormFieldAnswer;
import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.forms.persistence.ReservationFormResponseDao;
import com.reserly.platform.forms.persistence.ReservationFormResponseEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el adaptador transaccional entre el esquema publicado y los snapshots históricos. */
@ExtendWith(MockitoExtension.class)
class ReservationFormConfirmationServiceTests {

  @Mock private ReservationFormFieldDao fieldDao;
  @Mock private ReservationFormResponseDao responseDao;

  @Test
  void validatesNormalizesAndPersistsPublishedAnswers() {
    UUID venueId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID fieldId = UUID.randomUUID();
    Instant now = Instant.parse("2026-07-14T10:00:00Z");
    when(fieldDao.findAllPublishedByVenue(venueId)).thenReturn(List.of(field(fieldId, true)));
    var service = service();

    var result =
        service.validateAndPersist(
            venueId,
            reservationId,
            List.of(new ReservationFormFieldAnswer(fieldId, TextNode.valueOf("  Sin gluten  "))),
            now);

    assertThat(result)
        .singleElement()
        .satisfies(
            answer -> {
              assertThat(answer.fieldKey()).isEqualTo("allergies");
              assertThat(answer.value().textValue()).isEqualTo("Sin gluten");
            });
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<ReservationFormResponseEntity>> entities =
        ArgumentCaptor.forClass(Iterable.class);
    verify(responseDao).saveAll(entities.capture());
    assertThat(entities.getValue())
        .singleElement()
        .satisfies(
            entity -> {
              assertThat(entity.getReservationId()).isEqualTo(reservationId);
              assertThat(entity.getFieldId()).isEqualTo(fieldId);
              assertThat(entity.getFieldLabel()).isEqualTo("Alergias");
              assertThat(entity.getCreatedAt()).isEqualTo(now);
            });
  }

  @Test
  void rejectsMissingRequiredPublishedField() {
    UUID venueId = UUID.randomUUID();
    when(fieldDao.findAllPublishedByVenue(venueId))
        .thenReturn(List.of(field(UUID.randomUUID(), true)));

    assertThatThrownBy(
            () ->
                service().validateAndPersist(venueId, UUID.randomUUID(), List.of(), Instant.now()))
        .isInstanceOfSatisfying(
            ReservationFormResponseInvalidException.class,
            error ->
                assertThat(error.violation())
                    .isEqualTo(ReservationFormResponseViolation.MISSING_REQUIRED));
  }

  @Test
  void rejectsUnknownFieldIdentifier() {
    UUID venueId = UUID.randomUUID();
    when(fieldDao.findAllPublishedByVenue(venueId)).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service()
                    .validateAndPersist(
                        venueId,
                        UUID.randomUUID(),
                        List.of(
                            new ReservationFormFieldAnswer(
                                UUID.randomUUID(), TextNode.valueOf("x"))),
                        Instant.now()))
        .isInstanceOfSatisfying(
            ReservationFormResponseInvalidException.class,
            error ->
                assertThat(error.violation())
                    .isEqualTo(ReservationFormResponseViolation.UNKNOWN_FIELD));
  }

  private ReservationFormConfirmationServiceImpl service() {
    return new ReservationFormConfirmationServiceImpl(
        fieldDao, responseDao, new ReservationFormResponseValidatorImpl());
  }

  private ReservationFormFieldEntity field(UUID id, boolean required) {
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setId(id);
    field.setKey("allergies");
    field.setLabel("Alergias");
    field.setType(ReservationFormFieldType.SHORT_TEXT);
    field.setRequired(required);
    field.setActive(true);
    return field;
  }
}
