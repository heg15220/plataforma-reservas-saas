const SESSION_KEY = "reserly-demand-session-v1";
const CORRELATION_KEY = "reserly-demand-correlation-v1";

/** Inicia una nueva cadena de descubrimiento y conserva solo un UUID efímero por pestaña. */
export function startDemandCorrelation() {
  const requestId = crypto.randomUUID();
  if (typeof window !== "undefined") {
    try {
      window.sessionStorage.setItem(CORRELATION_KEY, requestId);
    } catch {
      // El UUID sigue siendo válido para esta llamada aunque storage esté bloqueado.
    }
  }
  return requestId;
}

/** Reutiliza la cadena actual entre navegación, disponibilidad, hold y resultado backend. */
export function getDemandCorrelationId() {
  if (typeof window === "undefined") return crypto.randomUUID();
  try {
    const existing = window.sessionStorage.getItem(CORRELATION_KEY);
    if (existing) return existing;
    return startDemandCorrelation();
  } catch {
    return crypto.randomUUID();
  }
}

/** Cabecera técnica; nunca actúa como credencial, identidad o permiso. */
export function demandCorrelationHeaders() {
  return { "X-Reserly-Correlation-Id": getDemandCorrelationId() };
}

/** Sesión efímera separada de la correlación de recorrido. */
export function demandSessionId() {
  try {
    const existing = window.sessionStorage.getItem(SESSION_KEY);
    if (existing) return existing;
    const created = crypto.randomUUID();
    window.sessionStorage.setItem(SESSION_KEY, created);
    return created;
  } catch {
    return crypto.randomUUID();
  }
}
