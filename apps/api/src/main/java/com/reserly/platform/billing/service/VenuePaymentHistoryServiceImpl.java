package com.reserly.platform.billing.service;

import com.reserly.platform.billing.dto.VenuePaymentHistoryItemResponse;
import com.reserly.platform.billing.dto.VenuePaymentHistoryResponse;
import com.reserly.platform.billing.persistence.PaymentDao;
import com.reserly.platform.billing.persistence.PaymentEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proyecta un historial acotado sin exponer hashes, payloads, firmas ni identificadores internos.
 */
@Service
public class VenuePaymentHistoryServiceImpl implements VenuePaymentHistoryService {

  static final int HISTORY_LIMIT = 50;

  private final VenueDao venueDao;
  private final PaymentDao paymentDao;

  public VenuePaymentHistoryServiceImpl(VenueDao venueDao, PaymentDao paymentDao) {
    this.venueDao = venueDao;
    this.paymentDao = paymentDao;
  }

  @Override
  @Transactional(readOnly = true)
  public VenuePaymentHistoryResponse findOwned(UUID ownerUserId) {
    if (ownerUserId == null) {
      throw new VenueSubscriptionNotFoundException();
    }
    UUID venueId =
        venueDao
            .findCurrentByOwnerUserId(ownerUserId)
            .orElseThrow(VenueSubscriptionNotFoundException::new)
            .getId();
    return new VenuePaymentHistoryResponse(
        paymentDao.findHistoryByVenueId(venueId, PageRequest.of(0, HISTORY_LIMIT)).stream()
            .map(this::toResponse)
            .toList());
  }

  private VenuePaymentHistoryItemResponse toResponse(PaymentEntity payment) {
    return new VenuePaymentHistoryItemResponse(
        payment.getProviderOrderId(),
        payment.getAmount(),
        payment.getCurrency(),
        payment.getStatus().persistedValue(),
        payment.getCreatedAt(),
        payment.getPaidAt());
  }
}
