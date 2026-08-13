import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  demandCorrelationHeaders,
  getDemandCorrelationId,
  startDemandCorrelation,
} from "./demand-correlation";
import { toDemandCode, trackDemandEvent } from "./demand-telemetry";
import { saveDemandConsent } from "@/features/privacy/demand-consent";

describe("demand telemetry", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    window.localStorage.clear();
    saveDemandConsent({ analytics: true, personalization: false, commercialActivation: false });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 200 })));
  });

  it("does not emit optional analytics when consent is absent", () => {
    window.localStorage.clear();
    trackDemandEvent("searchPerformed", { queryLength: 12 });
    expect(fetch).not.toHaveBeenCalled();
  });

  it("reuses an ephemeral session and never adds direct identifiers", async () => {
    trackDemandEvent("searchPerformed", { queryLength: 12 });
    trackDemandEvent("filterApplied", { filterCode: "searchFilters" });
    const calls = vi.mocked(fetch).mock.calls;
    const first = JSON.parse(String((calls[0]?.[1] as RequestInit).body));
    const second = JSON.parse(String((calls[1]?.[1] as RequestInit).body));
    expect(first.events[0].sessionId).toBe(second.events[0].sessionId);
    expect(first.events[0].requestId).toBe(second.events[0].requestId);
    expect(first.events[0]).not.toHaveProperty("anonymousId");
    expect(JSON.stringify(first)).not.toContain("email");
  });

  it("rotates correlation explicitly and exposes only a validated technical header", () => {
    const first = getDemandCorrelationId();
    const next = startDemandCorrelation();
    expect(next).not.toBe(first);
    expect(demandCorrelationHeaders()).toEqual({ "X-Reserly-Correlation-Id": next });
  });

  it("normalizes public slugs to governed codes", () => {
    expect(toDemandCode("spa-y-bienestar")).toBe("spaYBienestar");
    expect(toDemandCode("---")).toBe("unknown");
  });
});
