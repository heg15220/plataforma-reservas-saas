package com.reserly.platform.reviews.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reviews.persistence.ReviewAggregateProjection;
import com.reserly.platform.reviews.persistence.ReviewDao;
import com.reserly.platform.reviews.persistence.ReviewEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Cobertura de agregación, minimización, propiedad y límites de lectura de reseñas. */
class ReviewQueryServiceTests {

  private final ReviewDao reviewDao = mock(ReviewDao.class);
  private final VenueDao venueDao = mock(VenueDao.class);
  private final ReviewQueryService service = new ReviewQueryServiceImpl(reviewDao, venueDao);

  @Test
  void calculatesRoundedPublicSummaryAndReturnsRecentReviewsWithoutIdentity() {
    UUID venueId = UUID.randomUUID();
    ReviewEntity recent = review(venueId, 5, "Atención excelente.");
    ReviewEntity older = review(venueId, 4, null);
    var pageRequest = PageRequest.of(0, ReviewQueryServiceImpl.PUBLIC_LIMIT);
    var aggregate = aggregate(4.666, 25);
    when(reviewDao.summarizeByVenueId(venueId)).thenReturn(aggregate);
    when(reviewDao.findByVenueIdOrderByCreatedAtDescIdDesc(venueId, pageRequest))
        .thenReturn(new PageImpl<>(List.of(recent, older), pageRequest, 25));

    var response = service.findPublic(venueId);

    assertThat(response.averageRating()).isEqualByComparingTo("4.7");
    assertThat(response.reviewsCount()).isEqualTo(25);
    assertThat(response.truncated()).isTrue();
    assertThat(response.items()).extracting("rating").containsExactly(5, 4);
    assertThat(response.items())
        .allSatisfy(item -> assertThat(item.toString()).doesNotContain("@"));
  }

  @Test
  void representsVenueWithoutReviewsWithoutInventingAnAverage() {
    UUID venueId = UUID.randomUUID();
    var pageRequest = PageRequest.of(0, ReviewQueryServiceImpl.PUBLIC_LIMIT);
    var aggregate = aggregate(null, 0);
    when(reviewDao.summarizeByVenueId(venueId)).thenReturn(aggregate);
    when(reviewDao.findByVenueIdOrderByCreatedAtDescIdDesc(venueId, pageRequest))
        .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

    var response = service.findPublic(venueId);

    assertThat(response.averageRating()).isNull();
    assertThat(response.reviewsCount()).isZero();
    assertThat(response.truncated()).isFalse();
    assertThat(response.items()).isEmpty();
  }

  @Test
  void listsOnlyTheAuthenticatedOwnersVenueWithBoundedPagination() {
    UUID ownerId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();
    VenueEntity venue = new VenueEntity();
    venue.setId(venueId);
    ReviewEntity review = review(venueId, 3, "Correcto.");
    var pageRequest = PageRequest.of(0, 20);
    var aggregate = aggregate(3.0, 1);
    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.of(venue));
    when(reviewDao.summarizeByVenueId(venueId)).thenReturn(aggregate);
    when(reviewDao.findByVenueIdOrderByCreatedAtDescIdDesc(venueId, pageRequest))
        .thenReturn(new PageImpl<>(List.of(review), pageRequest, 1));

    var response = service.findOwned(ownerId, 0, 20);

    assertThat(response.averageRating()).isEqualByComparingTo("3.0");
    assertThat(response.reviewsCount()).isEqualTo(1);
    assertThat(response.items()).hasSize(1);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    verify(venueDao).findCurrentByOwnerUserId(ownerId);
  }

  @Test
  void rejectsInvalidPageOrMissingOwnedVenueBeforeReviewQueries() {
    UUID ownerId = UUID.randomUUID();

    assertThatThrownBy(() -> service.findOwned(ownerId, -1, 20))
        .isInstanceOf(VenueReviewInvalidPageException.class);
    assertThatThrownBy(() -> service.findOwned(ownerId, 0, 101))
        .isInstanceOf(VenueReviewInvalidPageException.class);
    verify(venueDao, never()).findCurrentByOwnerUserId(any());

    when(venueDao.findCurrentByOwnerUserId(ownerId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.findOwned(ownerId, 0, 20))
        .isInstanceOf(VenueReviewNotFoundException.class);
    verify(reviewDao, never()).summarizeByVenueId(any());
  }

  private ReviewAggregateProjection aggregate(Double average, long count) {
    ReviewAggregateProjection projection = mock(ReviewAggregateProjection.class);
    when(projection.getAverageRating()).thenReturn(average);
    when(projection.getReviewsCount()).thenReturn(count);
    return projection;
  }

  private ReviewEntity review(UUID venueId, int rating, String comment) {
    ReviewEntity review = new ReviewEntity();
    review.setId(UUID.randomUUID());
    review.setVenueId(venueId);
    review.setReservationId(UUID.randomUUID());
    review.setCustomerEmailNormalized("private@example.com");
    review.setRating(rating);
    review.setComment(comment);
    review.setCreatedAt(Instant.parse("2026-07-28T10:00:00Z"));
    review.setUpdatedAt(review.getCreatedAt());
    return review;
  }
}
