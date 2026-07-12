package com.reserly.platform.resources.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.resources.converter.EmployeeResourceConverter;
import com.reserly.platform.resources.dto.EmployeeResourceRequest;
import com.reserly.platform.resources.dto.EmployeeResourceResponse;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.service.EmployeeResourceCatalogService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que deriva siempre el local desde la cuenta autenticada. */
@RestController
public class EmployeeResourceControllerImpl implements EmployeeResourceController {

  private final EmployeeResourceCatalogService resourceCatalogService;
  private final EmployeeResourceConverter converter;

  public EmployeeResourceControllerImpl(
      EmployeeResourceCatalogService resourceCatalogService, EmployeeResourceConverter converter) {
    this.resourceCatalogService = resourceCatalogService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<List<EmployeeResourceResponse>> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(toResponses(resourceCatalogService.list(account.userId())));
  }

  @Override
  public ResponseEntity<EmployeeResourceResponse> create(
      AuthenticatedAccount account, EmployeeResourceRequest request) {
    EmployeeResourceEntity resource =
        resourceCatalogService.create(account.userId(), converter.toCommand(request));
    return ResponseEntity.created(URI.create("/api/venue/me/team/" + resource.getId()))
        .body(converter.toResponse(resource));
  }

  @Override
  public ResponseEntity<EmployeeResourceResponse> update(
      AuthenticatedAccount account, UUID resourceId, EmployeeResourceRequest request) {
    return ResponseEntity.ok(
        converter.toResponse(
            resourceCatalogService.update(
                account.userId(), resourceId, converter.toCommand(request))));
  }

  private List<EmployeeResourceResponse> toResponses(List<EmployeeResourceEntity> resources) {
    return resources.stream().map(converter::toResponse).toList();
  }
}
