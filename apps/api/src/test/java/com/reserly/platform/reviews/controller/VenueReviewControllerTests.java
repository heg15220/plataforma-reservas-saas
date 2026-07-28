package com.reserly.platform.reviews.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reviews.dto.VenueReviewListResponse;
import com.reserly.platform.reviews.service.ReviewQueryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Verifica que el endpoint privado delega únicamente con el propietario del principal. */
class VenueReviewControllerTests {

  @Test
  void returnsOwnedReviewPage() {
    ReviewQueryService service = org.mockito.Mockito.mock(ReviewQueryService.class);
    UUID ownerId = UUID.randomUUID();
    AuthenticatedAccount account =
        new AuthenticatedAccount(
            ownerId,
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("ROLE_VENUE_OWNER"));
    VenueReviewListResponse response =
        new VenueReviewListResponse(new BigDecimal("4.8"), 5, List.of(), 0, 20, 1);
    when(service.findOwned(ownerId, 0, 20)).thenReturn(response);

    var result = new VenueReviewControllerImpl(service).list(account, 0, 20);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isSameAs(response);
    verify(service).findOwned(ownerId, 0, 20);
  }
}
