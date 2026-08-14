package com.reserly.platform.demand.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.demand.ingestion.DemandEventIngestionServiceImpl;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

/** Compara los artefactos JSON fuente con los contratos efectivos empaquetados por Spring. */
class DemandFoundationContractTests {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @SuppressWarnings("unchecked")
  void javaIngestionSupportsExactlyTheVersionedJsonCatalog() throws IOException {
    Map<String, Object> catalog = read("catalog/event-catalog.v1.json");
    List<Map<String, Object>> events = (List<Map<String, Object>>) catalog.get("events");
    Set<String> jsonTypes = new HashSet<>();
    for (Map<String, Object> event : events) jsonTypes.add((String) event.get("type"));

    assertThat(jsonTypes).hasSize(22);
    assertThat(DemandEventIngestionServiceImpl.supportedEventTypes())
        .containsExactlyInAnyOrderElementsOf(jsonTypes);
    assertThat((List<String>) catalog.get("forbiddenFields"))
        .contains("email", "phone", "fingerprint", "rawQuery", "payload");
  }

  @Test
  @SuppressWarnings("unchecked")
  void packagedOntologyKeepsBilingualAttributesAndProhibitionsDisjoint() throws IOException {
    Map<String, Object> ontology = read("ontology/personal-care.v1.json");
    List<Map<String, Object>> attributes = (List<Map<String, Object>>) ontology.get("attributes");
    List<Map<String, Object>> prohibited =
        (List<Map<String, Object>>) ontology.get("prohibitedAttributes");
    Set<String> publishedCodes = new HashSet<>();
    for (Map<String, Object> attribute : attributes) {
      publishedCodes.add((String) attribute.get("code"));
      Map<String, String> name = (Map<String, String>) attribute.get("name");
      Map<String, String> definition = (Map<String, String>) attribute.get("definition");
      assertThat(name.get("es")).isNotBlank();
      assertThat(name.get("en")).isNotBlank();
      assertThat(definition.get("es")).isNotBlank();
      assertThat(definition.get("en")).isNotBlank();
    }
    Set<String> prohibitedCodes = new HashSet<>();
    for (Map<String, Object> attribute : prohibited) {
      prohibitedCodes.add((String) attribute.get("code"));
    }

    assertThat(attributes).hasSize(44);
    assertThat(prohibitedCodes).contains("medicalCondition", "psychologicalProfile");
    assertThat(publishedCodes).doesNotContainAnyElementsOf(prohibitedCodes);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> read(String path) throws IOException {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return objectMapper.readValue(input, Map.class);
    }
  }
}
