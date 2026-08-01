package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueSearchSuggestionProjection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica límites, normalización y minimización del autocompletado público. */
@ExtendWith(MockitoExtension.class)
class VenueSearchSuggestionServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueSearchSuggestionProjection projection;

  private VenueSearchSuggestionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new VenueSearchSuggestionServiceImpl(venueDao);
  }

  @Test
  void avoidsDatabaseQueriesUntilTwoCharactersExist() {
    var response = service.suggest(SupportedLocale.ES, "query", " a ", 8);

    assertThat(response.locale()).isEqualTo("es");
    assertThat(response.suggestions()).isEmpty();
    verifyNoInteractions(venueDao);
  }

  @Test
  void normalizesAccentsAndCapsQuerySuggestions() {
    when(projection.getValue()).thenReturn("Café Central");
    when(projection.getLabel()).thenReturn("Café Central");
    when(projection.getContext()).thenReturn("Restaurante · Madrid");
    when(venueDao.findPublishedQuerySuggestions("%cafe%", "cafe%", "cafe", "es", 10))
        .thenReturn(List.of(projection));

    var response = service.suggest(SupportedLocale.ES, "query", "  CAFÉ ", 100);

    assertThat(response.suggestions())
        .singleElement()
        .satisfies(
            suggestion -> {
              assertThat(suggestion.kind()).isEqualTo("query");
              assertThat(suggestion.value()).isEqualTo("Café Central");
              assertThat(suggestion.context()).isEqualTo("Restaurante · Madrid");
            });
  }

  @Test
  void returnsDistinctLocationProjectionWithLocalizedContext() {
    when(projection.getValue()).thenReturn("Madrid");
    when(projection.getLabel()).thenReturn("Madrid");
    when(projection.getContext()).thenReturn("city");
    when(venueDao.findPublishedLocationSuggestions("%mad%", "mad%", "mad", 8))
        .thenReturn(List.of(projection));

    var response = service.suggest(SupportedLocale.EN, "location", "Mad", 0);

    assertThat(response.suggestions())
        .singleElement()
        .satisfies(
            suggestion -> {
              assertThat(suggestion.kind()).isEqualTo("location");
              assertThat(suggestion.label()).isEqualTo("Madrid");
              assertThat(suggestion.context()).isEqualTo("City");
            });
  }
}
