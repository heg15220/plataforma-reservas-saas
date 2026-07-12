package com.reserly.platform.resources.service;

import com.reserly.platform.resources.dto.EmployeeResourceCommand;
import com.reserly.platform.resources.dto.EmployeeResourceWeeklyHoursRequest;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceHourEntity;
import java.util.List;
import java.util.UUID;

/** API interna para administrar empleados y recursos del local autenticado. */
public interface EmployeeResourceCatalogService {

  /** Lista recursos no archivados del local vigente del propietario. */
  List<EmployeeResourceEntity> list(UUID ownerUserId);

  /** Crea un empleado, profesional o recurso asociado al local vigente. */
  EmployeeResourceEntity create(UUID ownerUserId, EmployeeResourceCommand command);

  /** Edita un recurso propio no archivado o lo archiva de forma terminal. */
  EmployeeResourceEntity update(UUID ownerUserId, UUID resourceId, EmployeeResourceCommand command);

  /** Lista el horario semanal basico de un recurso propio no archivado. */
  List<EmployeeResourceHourEntity> listWeeklyHours(UUID ownerUserId, UUID resourceId);

  /** Reemplaza atomicamente el horario semanal basico de un recurso propio. */
  List<EmployeeResourceHourEntity> replaceWeeklyHours(
      UUID ownerUserId, UUID resourceId, EmployeeResourceWeeklyHoursRequest request);
}
