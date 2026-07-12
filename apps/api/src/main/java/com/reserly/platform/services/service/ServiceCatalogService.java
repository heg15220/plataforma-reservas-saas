package com.reserly.platform.services.service;

import com.reserly.platform.services.dto.ServiceCommand;
import com.reserly.platform.services.dto.ServiceResourceAssignmentRequest;
import com.reserly.platform.services.persistence.ServiceEntity;
import java.util.List;
import java.util.UUID;

/** API interna para listar, crear y editar servicios del local autenticado. */
public interface ServiceCatalogService {

  /** Lista todos los servicios del local vigente del propietario. */
  List<ServiceEntity> list(UUID ownerUserId);

  /** Crea un servicio asociado al local vigente del propietario. */
  ServiceEntity create(UUID ownerUserId, ServiceCommand command);

  /** Edita un servicio propio existente sin permitir reasignacion de local. */
  ServiceEntity update(UUID ownerUserId, UUID serviceId, ServiceCommand command);

  /** Reemplaza los recursos compatibles de un servicio propio con IDs del mismo local. */
  ServiceEntity replaceCompatibleResources(
      UUID ownerUserId, UUID serviceId, ServiceResourceAssignmentRequest request);
}
