package com.reserly.platform.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

/** Verifica que el esquema base sea completo, obligatorio, estable e inmutable. */
class ReservationBaseFieldCatalogTests {

  @Test
  void exposesEveryMandatorySystemFieldInStableOrder() {
    var fields = ReservationBaseFieldCatalog.fields();

    assertThat(fields)
        .extracting(ReservationBaseFieldDefinition::key)
        .containsExactly(
            "customer_name",
            "customer_email",
            "party_size",
            "reservation_date",
            "time_slot");
    assertThat(fields)
        .extracting(ReservationBaseFieldDefinition::inputType)
        .containsExactly("short_text", "email", "number", "date", "time_slot");
    assertThat(fields).allSatisfy(field -> assertThat(field.required()).isTrue());
    assertThat(fields).allSatisfy(field -> assertThat(field.editable()).isFalse());
  }

  @Test
  void keepsKeysLabelsAndPositionsUnique() {
    var fields = ReservationBaseFieldCatalog.fields();

    assertThat(fields).extracting(ReservationBaseFieldDefinition::position).containsExactly(0, 1, 2, 3, 4);
    assertThat(new HashSet<>(fields.stream().map(ReservationBaseFieldDefinition::key).toList()))
        .hasSameSizeAs(fields);
    assertThat(new HashSet<>(fields.stream().map(ReservationBaseFieldDefinition::labelKey).toList()))
        .hasSameSizeAs(fields);
  }

  @Test
  void doesNotAllowConsumersToMutateTheSystemSchema() {
    assertThatThrownBy(
            () ->
                ReservationBaseFieldCatalog.fields()
                    .add(
                        new ReservationBaseFieldDefinition(
                            "extra", "short_text", "reservation.form.extra", 5)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsIncompleteDefinitions() {
    assertThatThrownBy(
            () -> new ReservationBaseFieldDefinition("", "short_text", "reservation.form.name", 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new ReservationBaseFieldDefinition("name", "short_text", "", 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ReservationBaseFieldDefinition(
                    "name", "short_text", "reservation.form.name", -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}