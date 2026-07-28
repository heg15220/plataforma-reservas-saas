package com.reserly.platform.reviews.service;

import com.reserly.platform.reviews.dto.PublicReviewCollectionResponse;
import com.reserly.platform.reviews.dto.ReviewItemResponse;
import com.reserly.platform.reviews.dto.VenueReviewListResponse;
import com.reserly.platform.reviews.persistence.ReviewAggregateProjection;
import com.reserly.platform.reviews.persistence.ReviewDao;
import com.reserly.platform.reviews.persistence.ReviewEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula métricas bajo demanda y limita las lecturas de comentarios.
 *
 * <p>La ficha pública acredita publicación antes de invocar {@link #findPublic}. La consulta
 * privada resuelve el local desde el propietario autenticado y nunca acepta un {@code venueId}
 * enviado por el cliente.
 */
@Service
public class ReviewQueryServiceImpl implements ReviewQueryService {

  static final int PUBLIC_LIMIT = 20;
  private static final int MAX_PAGE = 100_000;
  private static final int MAX_SIZE = 100;

  private final ReviewDao reviewDao;
  private final VenueDao venueDao;

  public ReviewQueryServiceImpl(ReviewDao reviewDao, VenueDao venueDao) {
    this.reviewDao = reviewDao;
    this.venueDao = venueDao;
  }

  @Override
  @Transactional(readOnly = true)
  public PublicReviewCollectionResponse findPublic(UUID venueId) {
    ReviewAggregateProjection aggregate = reviewDao.summarizeByVenueId(venueId);
    Page<ReviewEntity> page =
        reviewDao.findByVenueIdOrderByCreatedAtDescIdDesc(venueId, PageRequest.of(0, PUBLIC_LIMIT));
    long count = count(aggregate);
    return new PublicReviewCollectionResponse(
        average(aggregate),
        count,
        count > page.getNumberOfElements(),
        page.map(this::toItem).toList());
  }

  @Override
  @Transactional(readOnly = true)
  public VenueReviewListResponse findOwned(UUID ownerUserId, int page, int size) {
    validatePage(ownerUserId, page, size);
    UUID venueId =
        venueDao
            .findCurrentByOwnerUserId(ownerUserId)
            .orElseThrow(VenueReviewNotFoundException::new)
            .getId();
    ReviewAggregateProjection aggregate = reviewDao.summarizeByVenueId(venueId);
    Page<ReviewEntity> reviews =
        reviewDao.findByVenueIdOrderByCreatedAtDescIdDesc(venueId, PageRequest.of(page, size));
    return new VenueReviewListResponse(
        average(aggregate),
        count(aggregate),
        reviews.map(this::toItem).toList(),
        page,
        size,
        reviews.getTotalPages());
  }

  private void validatePage(UUID ownerUserId, int page, int size) {
    if (ownerUserId == null || page < 0 || page > MAX_PAGE || size < 1 || size > MAX_SIZE) {
      throw new VenueReviewInvalidPageException();
    }
  }

  private ReviewItemResponse toItem(ReviewEntity review) {
    return new ReviewItemResponse(
        review.getId(), review.getRating(), review.getComment(), review.getCreatedAt());
  }

  private BigDecimal average(ReviewAggregateProjection aggregate) {
    if (aggregate == null || aggregate.getAverageRating() == null || count(aggregate) == 0) {
      return null;
    }
    return BigDecimal.valueOf(aggregate.getAverageRating()).setScale(1, RoundingMode.HALF_UP);
  }

  private long count(ReviewAggregateProjection aggregate) {
    return aggregate == null || aggregate.getReviewsCount() == null
        ? 0
        : aggregate.getReviewsCount();
  }
}
