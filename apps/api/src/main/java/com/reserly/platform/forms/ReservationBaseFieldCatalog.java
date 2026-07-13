package com.reserly.platform.forms;

import java.util.List;

/**
 * Fuente unica y ordenada de los campos obligatorios del formulario de reserva.
 *
 * <p>Estos campos no se persisten como configuracion personalizada: sus valores pertenecen al
 * agregado Reservation de la fase 7. El catalogo evita que un local pueda eliminarlos, reordenarlos
 * o volverlos opcionales.
 */
public final class ReservationBaseFieldCatalog {

  private static final List<ReservationBaseFieldDefinition> FIELDS =
      List.of(
          field("customer_name", "short_text", "reservation.form.customerName", 0),
          field("customer_email", "email", "reservation.form.customerEmail", 1),
          field("party_size", "number", "reservation.form.partySize", 2),
          field("reservation_date", "date", "reservation.form.date", 3),
          field("time_slot", "time_slot", "reservation.form.timeSlot", 4));

  private ReservationBaseFieldCatalog() {}

  /** Devuelve el esquema base inmutable en el orden de captura y confirmacion. */
  public static List<ReservationBaseFieldDefinition> fields() {
    return FIELDS;
  }

  private static ReservationBaseFieldDefinition field(
      String key, String inputType, String labelKey, int position) {
    return new ReservationBaseFieldDefinition(key, inputType, labelKey, position);
  }
}