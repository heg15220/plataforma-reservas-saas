package com.reserly.platform.businessverification.remote;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Registro validado y selección determinista de adaptadores.
 *
 * <p>La selección automática usa prioridad ascendente y código como desempate estable. Una
 * preferencia explícita nunca cae silenciosamente a otro proveedor.
 */
@Component
public class RemoteBusinessVerificationAdapterRegistry {

  private final Map<String, RemoteBusinessVerificationAdapter> adaptersByCode;

  public RemoteBusinessVerificationAdapterRegistry(
      List<RemoteBusinessVerificationAdapter> adapters) {
    Map<String, RemoteBusinessVerificationAdapter> registered = new HashMap<>();
    for (RemoteBusinessVerificationAdapter adapter : adapters) {
      validateDescriptor(adapter);
      RemoteBusinessVerificationAdapter previous = registered.put(adapter.providerCode(), adapter);
      if (previous != null) {
        throw new IllegalStateException("Duplicate remote verification provider code");
      }
    }
    adaptersByCode = Map.copyOf(registered);
  }

  /** Resuelve un proveedor compatible o falla sin efectuar red. */
  public RemoteBusinessVerificationAdapter resolve(
      RemoteBusinessVerificationRequest request, String preferredProvider) {
    if (preferredProvider != null && !preferredProvider.isBlank()) {
      String normalizedProvider = preferredProvider.strip().toLowerCase(Locale.ROOT);
      RemoteBusinessVerificationAdapter adapter = adaptersByCode.get(normalizedProvider);
      if (adapter == null || !adapter.supports(request)) {
        throw new NoRemoteVerificationAdapterException();
      }
      return adapter;
    }

    return adaptersByCode.values().stream()
        .filter(adapter -> adapter.supports(request))
        .min(
            Comparator.comparingInt(RemoteBusinessVerificationAdapter::priority)
                .thenComparing(RemoteBusinessVerificationAdapter::providerCode))
        .orElseThrow(NoRemoteVerificationAdapterException::new);
  }

  private void validateDescriptor(RemoteBusinessVerificationAdapter adapter) {
    String providerCode = adapter.providerCode();
    if (providerCode == null || !providerCode.matches("[a-z0-9][a-z0-9._-]{1,63}")) {
      throw new IllegalStateException("Remote verification provider code is invalid");
    }
    Set<String> countries = adapter.supportedCountries();
    if (countries == null
        || countries.isEmpty()
        || countries.stream().anyMatch(country -> !country.matches("[A-Z]{2}"))) {
      throw new IllegalStateException("Remote verification provider countries are invalid");
    }
    if (adapter.priority() < 0) {
      throw new IllegalStateException("Remote verification provider priority is invalid");
    }
  }
}
