import { beforeEach, describe, expect, it, vi } from "vitest";

import { toDemandCode, trackDemandEvent } from "./demand-telemetry";

describe("demand telemetry", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 200 })));
  });

  it("reuses an ephemeral session and never adds direct identifiers", async () => {
    trackDemandEvent("searchPerformed", { queryLength: 12 });
    trackDemandEvent("filterApplied", { filterCode: "searchFilters" });
    const calls = vi.mocked(fetch).mock.calls;
    const first = JSON.parse(String((calls[0]?.[1] as RequestInit).body));
    const second = JSON.parse(String((calls[1]?.[1] as RequestInit).body));
    expect(first.events[0].sessionId).toBe(second.events[0].sessionId);
    expect(first.events[0]).not.toHaveProperty("anonymousId");
    expect(JSON.stringify(first)).not.toContain("email");
  });

  it("normalizes public slugs to governed codes", () => {
    expect(toDemandCode("spa-y-bienestar")).toBe("spaYBienestar");
    expect(toDemandCode("---")).toBe("unknown");
  });
});
