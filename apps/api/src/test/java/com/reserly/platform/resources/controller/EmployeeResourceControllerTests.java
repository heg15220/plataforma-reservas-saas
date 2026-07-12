package com.reserly.platform.resources.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.resources.converter.EmployeeResourceConverter;
import com.reserly.platform.resources.dto.EmployeeResourceRequest;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.service.EmployeeResourceCatalogService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el contrato REST privado de equipo y recursos. */
@ExtendWith(MockitoExtension.class)
class EmployeeResourceControllerTests {

  @Mock private EmployeeResourceCatalogService resourceCatalogService;

  private EmployeeResourceControllerImpl controller;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller =
        new EmployeeResourceControllerImpl(resourceCatalogService, new EmployeeResourceConverter());
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void createsListsAndUpdatesResourcesForTheAuthenticatedOwner() {
    EmployeeResourceEntity resource = resource("active", true);
    EmployeeResourceRequest request = request("active", true);
    EmployeeResourceConverter converter = new EmployeeResourceConverter();
    when(resourceCatalogService.create(account.userId(), converter.toCommand(request)))
        .thenReturn(resource);
    when(resourceCatalogService.list(account.userId())).thenReturn(List.of(resource));
    when(resourceCatalogService.update(
            account.userId(), resource.getId(), converter.toCommand(request)))
        .thenReturn(resource);

    var created = controller.create(account, request);
    var listed = controller.list(account);
    var updated = controller.update(account, resource.getId(), request);

    assertThat(created.getHeaders().getLocation())
        .hasToString("/api/venue/me/team/" + resource.getId());
    assertThat(created.getBody().firstName()).isEqualTo("Ana");
    assertThat(created.getBody().status()).isEqualTo("active");
    assertThat(listed.getBody()).hasSize(1);
    assertThat(updated.getBody().publicVisibility()).isTrue();
  }

  @Test
  void mapsResourceErrorsWithoutLeakingDetails() {
    EmployeeResourceExceptionHandler handler = new EmployeeResourceExceptionHandler();

    assertThat(handler.handleInvalid().getBody().code()).isEqualTo("TEAM_RESOURCE_INVALID");
    assertThat(handler.handleNotFound().getBody().code()).isEqualTo("TEAM_RESOURCE_NOT_FOUND");
  }

  @Test
  void usesOnlyTheAuthenticatedOwnerBoundary() {
    EmployeeResourceRequest request = request("inactive", false);
    EmployeeResourceEntity resource = resource("inactive", false);
    when(resourceCatalogService.create(
            account.userId(), new EmployeeResourceConverter().toCommand(request)))
        .thenReturn(resource);

    controller.create(account, request);

    verify(resourceCatalogService)
        .create(account.userId(), new EmployeeResourceConverter().toCommand(request));
  }

  private EmployeeResourceRequest request(String status, boolean publicVisibility) {
    return new EmployeeResourceRequest(
        "employee",
        "Ana",
        "López",
        "Ana L.",
        "https://example.invalid/ana.png",
        "Peluquería",
        "Especialista en cortes clásicos",
        status,
        publicVisibility,
        "Nota privada");
  }

  private EmployeeResourceEntity resource(String status, boolean publicVisibility) {
    EmployeeResourceEntity entity = new EmployeeResourceEntity();
    entity.setId(UUID.randomUUID());
    entity.setType("employee");
    entity.setFirstName("Ana");
    entity.setLastName("López");
    entity.setPublicAlias("Ana L.");
    entity.setPhotoUrl("https://example.invalid/ana.png");
    entity.setSpecialty("Peluquería");
    entity.setDescription("Especialista en cortes clásicos");
    entity.setStatus(status);
    entity.setPublicVisibility(publicVisibility);
    entity.setInternalNotes("Nota privada");
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    return entity;
  }
}
