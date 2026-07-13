package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormFieldCommand;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import java.util.List;
import java.util.UUID;

/** Casos de uso privados para gestionar campos personalizados del formulario de reserva. */
public interface ReservationFormFieldService {
  List<ReservationFormFieldEntity> list(UUID ownerUserId);

  ReservationFormFieldEntity create(UUID ownerUserId, ReservationFormFieldCommand command);

  ReservationFormFieldEntity update(
      UUID ownerUserId, UUID fieldId, ReservationFormFieldCommand command);

  /**
   * Reemplaza el orden completo de campos activos en una sola transacción.
   *
   * @throws ReservationFormFieldInvalidException si faltan, sobran o se repiten identificadores
   */
  List<ReservationFormFieldEntity> reorder(UUID ownerUserId, List<UUID> fieldIds);

  void delete(UUID ownerUserId, UUID fieldId);
}
