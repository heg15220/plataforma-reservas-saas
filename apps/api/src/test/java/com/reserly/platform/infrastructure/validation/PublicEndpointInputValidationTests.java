package com.reserly.platform.infrastructure.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reserly.platform.reservations.controller.ReservationManagementControllerImpl;
import com.reserly.platform.reservations.service.ReservationManagementService;
import com.reserly.platform.venues.controller.VenuePublicSearchControllerImpl;
import com.reserly.platform.venues.service.VenuePublicSearchService;
import com.reserly.platform.venues.service.VenueSearchSuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Verifica límites HTTP públicos antes de ejecutar servicios o consultas. */
class PublicEndpointInputValidationTests {

  @Test
  void rejectsOversizedAndOutOfRangeSearchInputsBeforeServiceExecution() throws Exception {
    VenuePublicSearchService searchService = mock(VenuePublicSearchService.class);
    VenueSearchSuggestionService suggestionService = mock(VenueSearchSuggestionService.class);
    MockMvc mockMvc =
        mockMvc(new VenuePublicSearchControllerImpl(searchService, suggestionService));

    mockMvc
        .perform(
            get("/api/public/venues/search")
                .param("q", "a".repeat(161))
                .param("latitude", "91")
                .param("page", "10001")
                .param("size", "51"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("REQUEST_INVALID"));

    verifyNoInteractions(searchService, suggestionService);
  }

  @Test
  void rejectsMalformedManagementTokenBeforeHashingOrPersistence() throws Exception {
    ReservationManagementService service = mock(ReservationManagementService.class);
    MockMvc mockMvc = mockMvc(new ReservationManagementControllerImpl(service));

    mockMvc
        .perform(get("/api/public/reservations/manage/not-a-token"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("REQUEST_INVALID"));

    verifyNoInteractions(service);
  }

  private MockMvc mockMvc(Object controller) {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    return MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new RequestValidationExceptionHandler())
        .setValidator(validator)
        .build();
  }
}
