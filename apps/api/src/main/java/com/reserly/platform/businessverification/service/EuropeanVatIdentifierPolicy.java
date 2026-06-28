package com.reserly.platform.businessverification.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Clasifica si una identidad debe consultarse en VIES.
 *
 * <p>Para España se exige que el usuario haya aportado el prefijo {@code ES}; un NIF nacional sin
 * ese indicio no se interpreta como alta en ROI. En los demás territorios VIES soportados, el
 * identificador empresarial se trata inicialmente como VAT ID porque todavía no hay adaptadores
 * registrales nacionales.
 */
@Component
public class EuropeanVatIdentifierPolicy {

  private static final Set<String> VIES_TERRITORIES =
      Set.of(
          "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE",
          "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK", "XI");

  /** Devuelve {@code true} si la cuenta debe pasar por el adaptador VIES. */
  public boolean isEuVatIdentifier(BusinessAccountEntity account) {
    if (!VIES_TERRITORIES.contains(account.getTaxCountry())) {
      return false;
    }
    if (!"ES".equals(account.getTaxCountry())) {
      return true;
    }
    return compact(account.getBusinessTaxIdentifier()).startsWith("ES");
  }

  private String compact(String value) {
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toUpperCase(Locale.ROOT);
    StringBuilder compact = new StringBuilder(normalized.length());
    for (int index = 0; index < normalized.length(); index++) {
      char current = normalized.charAt(index);
      if (current >= 'A' && current <= 'Z' || current >= '0' && current <= '9') {
        compact.append(current);
      }
    }
    return compact.toString();
  }
}
