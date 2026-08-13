package com.reserly.platform.demand.correlation;

import java.util.UUID;

/** Consulta de cobertura por correlación técnica sin exponer payloads analíticos. */
public interface DemandEventReconciliationService {

  /** Reconcilia eventos aceptados preservando el backend como autoridad de resultados. */
  DemandEventReconciliation reconcile(UUID requestId);
}
