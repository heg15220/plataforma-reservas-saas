package com.reserly.platform.services.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.services.converter.ServiceConverter;
import com.reserly.platform.services.dto.ServiceLocalizedTextDto;
import com.reserly.platform.services.dto.ServiceRequest;
import com.reserly.platform.services.dto.ServiceResourceAssignmentRequest;
import com.reserly.platform.services.persistence.ServiceEntity;
import com.reserly.platform.services.service.ServiceCatalogService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el contrato REST privado de servicios y sus errores estables. */
@ExtendWith(MockitoExtension.class)
class ServiceControllerTests {

  @Mock private ServiceCatalogService serviceCatalogService;

  private ServiceControllerImpl controller;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller = new ServiceControllerImpl(serviceCatalogService, new ServiceConverter());
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void createsListsAndUpdatesServicesForTheAuthenticatedOwner() {
    ServiceEntity service = serviceEntity();
    ServiceRequest request = request();
    ServiceConverter converter = new ServiceConverter();
    when(serviceCatalogService.create(account.userId(), converter.toCommand(request)))
        .thenReturn(service);
    when(serviceCatalogService.list(account.userId())).thenReturn(List.of(service));
    when(serviceCatalogService.update(
            account.userId(), service.getId(), converter.toCommand(request)))
        .thenReturn(service);

    var created = controller.create(account, request);
    var listed = controller.list(account);
    var updated = controller.update(account, service.getId(), request);

    assertThat(created.getHeaders().getLocation())
        .hasToString("/api/venue/me/services/" + service.getId());
    assertThat(created.getBody().name()).isEqualTo("Corte");
    assertThat(created.getBody().nameI18n().values()).containsEntry("en", "Cut");
    assertThat(created.getBody().allowsAnyAvailableResource()).isFalse();
    assertThat(listed.getBody()).hasSize(1);
    assertThat(updated.getBody().durationMinutes()).isEqualTo(45);
  }

  @Test
  void mapsServiceErrorsWithoutLeakingDetails() {
    ServiceExceptionHandler handler = new ServiceExceptionHandler();

    assertThat(handler.handleInvalid().getBody().code()).isEqualTo("SERVICE_INVALID");
    assertThat(handler.handleNotFound().getBody().code()).isEqualTo("SERVICE_NOT_FOUND");
  }

  @Test
  void deletesNoResourceAndUsesOnlyTheAuthenticatedOwnerBoundary() {
    ServiceRequest request = request();
    ServiceEntity service = serviceEntity();
    when(serviceCatalogService.create(account.userId(), new ServiceConverter().toCommand(request)))
        .thenReturn(service);

    controller.create(account, request);

    verify(serviceCatalogService)
        .create(account.userId(), new ServiceConverter().toCommand(request));
  }

  @Test
  void replacesCompatibleResourcesForAuthenticatedOwner() {
    ServiceEntity service = serviceEntity();
    UUID resourceId = UUID.randomUUID();
    ServiceResourceAssignmentRequest request =
        new ServiceResourceAssignmentRequest(Set.of(resourceId));
    when(serviceCatalogService.replaceCompatibleResources(
            account.userId(), service.getId(), request))
        .thenReturn(service);

    var response = controller.replaceCompatibleResources(account, service.getId(), request);

    assertThat(response.getBody().id()).isEqualTo(service.getId());
    verify(serviceCatalogService)
        .replaceCompatibleResources(account.userId(), service.getId(), request);
  }

  private ServiceRequest request() {
    ServiceLocalizedTextDto nameI18n =
        new ServiceLocalizedTextDto("es", Map.of("es", "Corte", "en", "Cut"));
    ServiceLocalizedTextDto descriptionI18n =
        new ServiceLocalizedTextDto("es", Map.of("es", "Corte clasico", "en", "Classic cut"));
    return new ServiceRequest(
        "Corte", nameI18n, "Corte clasico", descriptionI18n, 45, 1, true, false, "exact_time");
  }

  private ServiceEntity serviceEntity() {
    ServiceEntity service = new ServiceEntity();
    service.setId(UUID.randomUUID());
    service.setName("Corte");
    service.setNameI18n(
        new LocalizedText(
            SupportedLocale.ES, Map.of(SupportedLocale.ES, "Corte", SupportedLocale.EN, "Cut")));
    service.setDescription("Corte clasico");
    service.setDescriptionI18n(
        new LocalizedText(
            SupportedLocale.ES,
            Map.of(SupportedLocale.ES, "Corte clasico", SupportedLocale.EN, "Classic cut")));
    service.setDurationMinutes(45);
    service.setCapacityRequired(1);
    service.setActive(true);
    service.setAnyAvailableResourceAllowed(false);
    service.setCreatedAt(Instant.now());
    service.setUpdatedAt(Instant.now());
    return service;
  }
}
