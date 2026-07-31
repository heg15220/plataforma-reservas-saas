package com.reserly.platform.services.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Contratos de persistencia que protegen la interoperabilidad de servicios con Hibernate. */
class ServiceEntityTests {

  @Test
  void preservesTheCollectionInstanceProvidedByThePersistenceProvider() {
    ServiceEntity service = new ServiceEntity();
    Set<EmployeeResourceEntity> managedCollection = new LinkedHashSet<>();

    service.setCompatibleResources(managedCollection);

    assertThat(service.getCompatibleResources()).isSameAs(managedCollection);
  }
}
