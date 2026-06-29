package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/** Determina motivo y alternativas documentales sin texto libre ni decisiones del cliente. */
@Component
public class BusinessVerificationDocumentRequestPolicy {

  private static final List<BusinessVerificationDocumentType> SPAIN_TYPES =
      List.of(
          BusinessVerificationDocumentType.CENSUS_REGISTRATION_036_037,
          BusinessVerificationDocumentType.CENSUS_CERTIFICATE,
          BusinessVerificationDocumentType.ACTIVITY_OR_OPENING_LICENSE,
          BusinessVerificationDocumentType.EQUIVALENT_ADMINISTRATIVE_DOCUMENT,
          BusinessVerificationDocumentType.OTHER);

  private static final List<BusinessVerificationDocumentType> INTERNATIONAL_TYPES =
      List.of(
          BusinessVerificationDocumentType.EQUIVALENT_ADMINISTRATIVE_DOCUMENT,
          BusinessVerificationDocumentType.OTHER);

  /** Clasifica el motivo más específico disponible en la evidencia mínima. */
  public BusinessVerificationDocumentRequestReason reason(
      BusinessAccountEntity account, BusinessVerificationCheckEntity check) {
    if ("aeat-census-manual".equals(check.getProvider())) {
      return BusinessVerificationDocumentRequestReason.NO_AUTOMATED_CHANNEL;
    }
    if ("error".equals(check.getStatus())) {
      return BusinessVerificationDocumentRequestReason.PROVIDER_UNAVAILABLE;
    }
    if ("verified".equals(check.getStatus()) && !Boolean.TRUE.equals(check.getMatchedLegalName())) {
      return BusinessVerificationDocumentRequestReason.LEGAL_NAME_UNCONFIRMED;
    }
    if ("verified".equals(check.getStatus())
        && account.getBusinessAddress() != null
        && !account.getBusinessAddress().isBlank()
        && !Boolean.TRUE.equals(check.getMatchedAddress())) {
      return BusinessVerificationDocumentRequestReason.ADDRESS_UNCONFIRMED;
    }
    return BusinessVerificationDocumentRequestReason.INSUFFICIENT_PROVIDER_DATA;
  }

  /** Tipos admitidos según el país; la licencia española es evidencia complementaria. */
  public List<BusinessVerificationDocumentType> requestedTypes(BusinessAccountEntity account) {
    return "ES".equals(account.getTaxCountry()) ? SPAIN_TYPES : INTERNATIONAL_TYPES;
  }
}
