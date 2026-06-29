package com.reserly.platform.businessverification.service;

/** Motivos cerrados y no sensibles que impiden publicar locales de una cuenta empresarial. */
public enum VenuePublicationBlocker {
  EMAIL_NOT_VERIFIED,
  ACCOUNT_TYPE_NOT_VENUE_BUSINESS,
  TAX_IDENTIFIER_NOT_NORMALIZED,
  BUSINESS_VERIFICATION_NOT_APPROVED
}
