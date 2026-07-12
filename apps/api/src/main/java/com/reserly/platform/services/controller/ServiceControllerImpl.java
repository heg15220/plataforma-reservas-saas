package com.reserly.platform.services.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.services.converter.ServiceConverter;
import com.reserly.platform.services.dto.ServiceRequest;
import com.reserly.platform.services.dto.ServiceResourceAssignmentRequest;
import com.reserly.platform.services.dto.ServiceResponse;
import com.reserly.platform.services.persistence.ServiceEntity;
import com.reserly.platform.services.service.ServiceCatalogService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que usa la cuenta autenticada como unica frontera de propiedad. */
@RestController
public class ServiceControllerImpl implements ServiceController {

  private final ServiceCatalogService serviceCatalogService;
  private final ServiceConverter converter;

  public ServiceControllerImpl(
      ServiceCatalogService serviceCatalogService, ServiceConverter converter) {
    this.serviceCatalogService = serviceCatalogService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<List<ServiceResponse>> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(toResponses(serviceCatalogService.list(account.userId())));
  }

  @Override
  public ResponseEntity<ServiceResponse> create(
      AuthenticatedAccount account, ServiceRequest request) {
    ServiceEntity service =
        serviceCatalogService.create(account.userId(), converter.toCommand(request));
    return ResponseEntity.created(URI.create("/api/venue/me/services/" + service.getId()))
        .body(converter.toResponse(service));
  }

  @Override
  public ResponseEntity<ServiceResponse> update(
      AuthenticatedAccount account, UUID serviceId, ServiceRequest request) {
    return ResponseEntity.ok(
        converter.toResponse(
            serviceCatalogService.update(
                account.userId(), serviceId, converter.toCommand(request))));
  }

  @Override
  public ResponseEntity<ServiceResponse> replaceCompatibleResources(
      AuthenticatedAccount account, UUID serviceId, ServiceResourceAssignmentRequest request) {
    return ResponseEntity.ok(
        converter.toResponse(
            serviceCatalogService.replaceCompatibleResources(
                account.userId(), serviceId, request)));
  }

  private List<ServiceResponse> toResponses(List<ServiceEntity> services) {
    return services.stream().map(converter::toResponse).toList();
  }
}
