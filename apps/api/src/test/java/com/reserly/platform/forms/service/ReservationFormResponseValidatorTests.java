package com.reserly.platform.forms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.reserly.platform.forms.dto.ReservationFormAnswerCommand;
import com.reserly.platform.forms.dto.ReservationFormPreviewFieldResponse;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.dto.ValidatedReservationFormAnswer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica validación backend y normalización por tipo contra un esquema explícito. */
class ReservationFormResponseValidatorTests {

  private ReservationFormResponseValidator validator;
  private UUID slotId;

  @BeforeEach
  void setUp() {
    validator = new ReservationFormResponseValidatorImpl();
    slotId = UUID.randomUUID();
  }

  @Test
  void validatesAndNormalizesEverySupportedInputTypeInSchemaOrder() {
    ReservationFormPreviewResponse form = completeForm();
    List<ReservationFormAnswerCommand> answers =
        List.of(
            answer("notes", text(" <img src=x onerror=alert(1)>Llegar diez minutos antes ")),
            answer("customer_name", text(" Ana ")),
            answer("customer_email", text("ana@example.com")),
            answer("party_size", number(2)),
            answer("reservation_date", text("2026-08-01")),
            answer("time_slot", text(slotId.toString())),
            answer("score", decimal(4.5)),
            answer("area", text("Terraza")),
            answer("needs_access", bool(false)),
            answer("phone", text("+34 612 345 678")));

    List<ValidatedReservationFormAnswer> validated = validator.validate(form, answers);

    assertThat(validated)
        .extracting(ValidatedReservationFormAnswer::fieldKey)
        .containsExactly(
            "customer_name",
            "customer_email",
            "party_size",
            "reservation_date",
            "time_slot",
            "notes",
            "score",
            "area",
            "needs_access",
            "phone");
    assertThat(validated.get(0).value().textValue()).isEqualTo("Ana");
    assertThat(validated.get(5).value().textValue()).isEqualTo("Llegar diez minutos antes");
    assertThat(validated.get(8).value().booleanValue()).isFalse();
    assertThat(validated.get(5).fieldLabel()).isEqualTo("Notes");
  }

  @Test
  void omitsUnansweredOptionalFieldsButRequiresEveryMandatoryField() {
    ReservationFormPreviewResponse optionalForm =
        new ReservationFormPreviewResponse(List.of(field("comment", "long_text", false, null, 0)));

    assertThat(validator.validate(optionalForm, null)).isEmpty();

    ReservationFormPreviewResponse requiredForm =
        new ReservationFormPreviewResponse(List.of(field("comment", "long_text", true, null, 0)));

    assertViolation(
        requiredForm, List.of(), "comment", ReservationFormResponseViolation.MISSING_REQUIRED);
  }

  @Test
  void rejectsUnknownAndDuplicateAnswerKeys() {
    ReservationFormPreviewResponse form =
        new ReservationFormPreviewResponse(List.of(field("comment", "long_text", false, null, 0)));

    assertViolation(
        form,
        List.of(answer("unknown", text("Valor"))),
        "unknown",
        ReservationFormResponseViolation.UNKNOWN_FIELD);
    assertViolation(
        form,
        List.of(answer("comment", text("Uno")), answer("comment", text("Dos"))),
        "comment",
        ReservationFormResponseViolation.DUPLICATE_FIELD);
  }

  @Test
  void rejectsValuesWithWrongJsonType() {
    ReservationFormPreviewResponse form =
        new ReservationFormPreviewResponse(
            List.of(
                field("quantity", "number", true, null, 0),
                field("accepted", "checkbox", true, null, 1)));

    assertViolation(
        form,
        List.of(answer("quantity", text("2")), answer("accepted", bool(true))),
        "quantity",
        ReservationFormResponseViolation.INVALID_TYPE);
    assertViolation(
        form,
        List.of(answer("quantity", number(2)), answer("accepted", text("true"))),
        "accepted",
        ReservationFormResponseViolation.INVALID_TYPE);
  }

  @Test
  void rejectsInvalidFormatsAndValues() {
    assertSingleInvalidValue("email", "email", text("invalid"));
    assertSingleInvalidValue("phone", "phone", text("123"));
    assertSingleInvalidValue("date", "date", text("31/12/2026"));
    assertSingleInvalidValue("area", "select", text("No existe"), List.of("Interior"));
    assertSingleInvalidValue("time_slot", "time_slot", text("not-a-uuid"));

    ReservationFormPreviewResponse partySize =
        new ReservationFormPreviewResponse(List.of(field("party_size", "number", true, null, 0)));
    assertViolation(
        partySize,
        List.of(answer("party_size", number(0))),
        "party_size",
        ReservationFormResponseViolation.INVALID_VALUE);
  }

  @Test
  void rejectsBlankOrOversizedTextAndInvalidSchema() {
    assertSingleInvalidValue("name", "short_text", text(" "));
    assertSingleInvalidValue("notes", "long_text", text("x".repeat(4001)));

    ReservationFormPreviewFieldResponse duplicate = field("same", "short_text", true, null, 0);
    ReservationFormPreviewResponse invalidSchema =
        new ReservationFormPreviewResponse(List.of(duplicate, duplicate));

    assertInvalidSchema(invalidSchema);

    ReservationFormPreviewResponse unsupportedType =
        new ReservationFormPreviewResponse(List.of(field("optional", "currency", false, null, 0)));
    ReservationFormPreviewResponse selectorWithoutOptions =
        new ReservationFormPreviewResponse(List.of(field("choice", "select", false, null, 0)));

    assertInvalidSchema(unsupportedType);
    assertInvalidSchema(selectorWithoutOptions);
  }

  private void assertInvalidSchema(ReservationFormPreviewResponse form) {
    assertThatThrownBy(() -> validator.validate(form, List.of()))
        .isInstanceOfSatisfying(
            ReservationFormResponseInvalidException.class,
            exception ->
                assertThat(exception.violation())
                    .isEqualTo(ReservationFormResponseViolation.INVALID_SCHEMA));
  }

  private ReservationFormPreviewResponse completeForm() {
    List<ReservationFormPreviewFieldResponse> fields = new ArrayList<>();
    fields.add(field("customer_name", "short_text", true, null, 0));
    fields.add(field("customer_email", "email", true, null, 1));
    fields.add(field("party_size", "number", true, null, 2));
    fields.add(field("reservation_date", "date", true, null, 3));
    fields.add(field("time_slot", "time_slot", true, null, 4));
    fields.add(field("notes", "long_text", false, null, 5));
    fields.add(field("score", "number", true, null, 6));
    fields.add(field("area", "select", true, List.of("Interior", "Terraza"), 7));
    fields.add(field("needs_access", "checkbox", true, null, 8));
    fields.add(field("phone", "phone", true, null, 9));
    return new ReservationFormPreviewResponse(fields);
  }

  private void assertSingleInvalidValue(String key, String type, JsonNode value) {
    assertSingleInvalidValue(key, type, value, null);
  }

  private void assertSingleInvalidValue(
      String key, String type, JsonNode value, List<String> options) {
    ReservationFormPreviewResponse form =
        new ReservationFormPreviewResponse(List.of(field(key, type, true, options, 0)));
    assertViolation(
        form, List.of(answer(key, value)), key, ReservationFormResponseViolation.INVALID_VALUE);
  }

  private void assertViolation(
      ReservationFormPreviewResponse form,
      List<ReservationFormAnswerCommand> answers,
      String fieldKey,
      ReservationFormResponseViolation violation) {
    assertThatThrownBy(() -> validator.validate(form, answers))
        .isInstanceOfSatisfying(
            ReservationFormResponseInvalidException.class,
            exception -> {
              assertThat(exception.violation()).isEqualTo(violation);
              assertThat(exception.fieldKey()).isEqualTo(fieldKey);
            });
  }

  private ReservationFormPreviewFieldResponse field(
      String key, String type, boolean required, List<String> options, int position) {
    return new ReservationFormPreviewFieldResponse(
        UUID.randomUUID(),
        "custom",
        key,
        type,
        Character.toUpperCase(key.charAt(0)) + key.substring(1),
        null,
        null,
        required,
        true,
        options,
        null,
        position);
  }

  private ReservationFormAnswerCommand answer(String key, JsonNode value) {
    return new ReservationFormAnswerCommand(key, value);
  }

  private JsonNode text(String value) {
    return JsonNodeFactory.instance.textNode(value);
  }

  private JsonNode number(int value) {
    return JsonNodeFactory.instance.numberNode(value);
  }

  private JsonNode decimal(double value) {
    return JsonNodeFactory.instance.numberNode(value);
  }

  private JsonNode bool(boolean value) {
    return JsonNodeFactory.instance.booleanNode(value);
  }
}
