import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  DEMAND_CONSENT_STORAGE_KEY,
  hasDemandConsent,
  readDemandConsent,
  saveDemandConsent,
} from "./demand-consent";

describe("demand consent", () => {
  beforeEach(() => localStorage.clear());

  it("parte sin consentimiento y conserva cada finalidad por separado", () => {
    expect(readDemandConsent()).toBeNull();
    saveDemandConsent({ analytics: true, personalization: false, commercialActivation: false });
    expect(hasDemandConsent("analytics")).toBe(true);
    expect(hasDemandConsent("personalization")).toBe(false);
  });

  it("rechaza decisiones parciales o de una versión desconocida", () => {
    localStorage.setItem(DEMAND_CONSENT_STORAGE_KEY, JSON.stringify({ analytics: true }));
    expect(readDemandConsent()).toBeNull();
    localStorage.setItem(DEMAND_CONSENT_STORAGE_KEY, "not-json");
    expect(readDemandConsent()).toBeNull();
    vi.useRealTimers();
  });
});
