package com.reserly.platform.demand.attribute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeDao;
import com.reserly.platform.demand.attribute.persistence.DemandAttributeEntity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Materializa de forma idempotente el catálogo contractual personal-care.v1 tras Flyway.
 *
 * <p>El JSON sigue siendo la única fuente editorial: el inicializador solo lo proyecta a filas
 * publicadas cuando la tabla está vacía. Un entorno parcialmente poblado falla deliberadamente para
 * evitar mezclar seeds o sobrescribir decisiones administrativas.
 */
@Component
public class DemandOntologySeedInitializer implements ApplicationRunner {

  private static final String RESOURCE = "ontology/personal-care.v1.json";
  private final DemandAttributeDao attributeDao;
  private final ObjectMapper objectMapper;

  public DemandOntologySeedInitializer(DemandAttributeDao attributeDao, ObjectMapper objectMapper) {
    this.attributeDao = attributeDao;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments arguments) {
    if (attributeDao.count() > 0) {
      return;
    }
    OntologySeed seed = readSeed();
    Instant effectiveAt = seed.effectiveFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
    for (AttributeSeed source : seed.attributes()) {
      DemandAttributeEntity target = new DemandAttributeEntity();
      target.setOntologyVersion(seed.ontologyVersion());
      target.setCode(source.code());
      target.setFamily(source.family());
      target.setParentCode(source.parentCode());
      target.setAttributeType(source.type());
      target.setNameEs(source.name().es());
      target.setNameEn(source.name().en());
      target.setDefinitionEs(source.definition().es());
      target.setDefinitionEn(source.definition().en());
      target.setAllowedSources(source.allowedSources());
      target.setAllowedUses(source.allowedUses());
      target.setValidityMode(source.validity().mode());
      target.setTtlDays(source.validity().ttlDays());
      target.setMinimumEvidence(source.minimumEvidence());
      target.setGovernanceStatus("published");
      target.setPublishedAt(effectiveAt);
      target.setCreatedAt(effectiveAt);
      target.setUpdatedAt(effectiveAt);
      attributeDao.saveAndFlush(target);
    }
  }

  private OntologySeed readSeed() {
    try (var input = new ClassPathResource(RESOURCE).getInputStream()) {
      return objectMapper.readValue(input, OntologySeed.class);
    } catch (IOException exception) {
      throw new UncheckedIOException("No se pudo leer el seed gobernado " + RESOURCE, exception);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OntologySeed(
      String ontologyVersion, java.time.LocalDate effectiveFrom, List<AttributeSeed> attributes) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record AttributeSeed(
      String code,
      String family,
      String parentCode,
      LocalizedSeed name,
      LocalizedSeed definition,
      String type,
      List<String> allowedSources,
      ValiditySeed validity,
      List<String> allowedUses,
      int minimumEvidence,
      String status) {}

  private record LocalizedSeed(String es, String en) {}

  private record ValiditySeed(String mode, Integer ttlDays) {}
}
