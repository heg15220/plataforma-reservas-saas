/** Versión inmutable de la política granular del motor de demanda. */
export const DEMAND_CONSENT_VERSION = "demand-consent.v1";
export const DEMAND_CONSENT_STORAGE_KEY = "reserly:demand-consent:v1";

export interface DemandConsentChoices {
  analytics: boolean;
  personalization: boolean;
  commercialActivation: boolean;
}

export interface DemandConsentRecord extends DemandConsentChoices {
  version: typeof DEMAND_CONSENT_VERSION;
  decidedAt: string;
}

/** Lee una decisión íntegra; valores antiguos, parciales o manipulados se tratan como ausencia. */
export function readDemandConsent(): DemandConsentRecord | null {
  if (typeof window === "undefined") return null;
  try {
    const value: unknown = JSON.parse(
      window.localStorage.getItem(DEMAND_CONSENT_STORAGE_KEY) ?? "null",
    );
    if (!isConsentRecord(value)) return null;
    return value;
  } catch {
    return null;
  }
}

/** Persiste solo tres decisiones y emite un evento local para consumidores de la misma pestaña. */
export function saveDemandConsent(choices: DemandConsentChoices): DemandConsentRecord {
  const record: DemandConsentRecord = {
    ...choices,
    version: DEMAND_CONSENT_VERSION,
    decidedAt: new Date().toISOString(),
  };
  window.localStorage.setItem(DEMAND_CONSENT_STORAGE_KEY, JSON.stringify(record));
  window.dispatchEvent(new CustomEvent("reserly:demand-consent-changed", { detail: record }));
  return record;
}

/** Comprueba finalidad sin convertir la ausencia en consentimiento implícito. */
export function hasDemandConsent(purpose: keyof DemandConsentChoices) {
  return readDemandConsent()?.[purpose] === true;
}

function isConsentRecord(value: unknown): value is DemandConsentRecord {
  if (!value || typeof value !== "object") return false;
  const candidate = value as Partial<DemandConsentRecord>;
  return (
    candidate.version === DEMAND_CONSENT_VERSION &&
    typeof candidate.decidedAt === "string" &&
    !Number.isNaN(Date.parse(candidate.decidedAt)) &&
    typeof candidate.analytics === "boolean" &&
    typeof candidate.personalization === "boolean" &&
    typeof candidate.commercialActivation === "boolean"
  );
}
