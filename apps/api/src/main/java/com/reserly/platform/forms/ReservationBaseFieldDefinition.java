package com.reserly.platform.forms;

/**
 * Contrato inmutable de un campo base que siempre forma parte del formulario de reserva.
 *
 * @param key clave estable usada por contratos y validacion
 * @param inputType tipo de entrada del sistema
 * @param labelKey clave i18n que debe resolver el canal cliente
 * @param position orden fijo anterior a los campos personalizados
 */
public record ReservationBaseFieldDefinition(
    String key, String inputType, String labelKey, int position) {

  public ReservationBaseFieldDefinition {
    if (key == null
        || key.isBlank()
        || inputType == null
        || inputType.isBlank()
        || labelKey == null
        || labelKey.isBlank()
        || position < 0) {
      throw new IllegalArgumentException("La definicion del campo base no es valida");
    }
  }

  /** Los campos base nunca pueden convertirse en opcionales. */
  public boolean required() {
    return true;
  }

  /** Los campos base pertenecen al sistema y no son editables por el local. */
  public boolean editable() {
    return false;
  }
}