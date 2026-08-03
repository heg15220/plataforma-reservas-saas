import { describe, expect, it } from "vitest";

import { assessIncidentHistoryRisk } from "./incident-history-risk";

const NOW = new Date("2026-08-03T12:00:00Z");

describe("assessIncidentHistoryRisk", () => {
  it("returns low risk when there are no operational incidents", () => {
    expect(assessIncidentHistoryRisk([], NOW)).toEqual({
      level: "low",
      operationalCount: 0,
      recentCount: 0,
      daysSinceLastIncident: null,
    });
  });

  it("ignores dismissed incidents", () => {
    expect(
      assessIncidentHistoryRisk([{ reportedAt: "2026-08-01T12:00:00Z", status: "dismissed" }], NOW)
        .level,
    ).toBe("low");
  });

  it("returns low risk for one operational incident after 180 days", () => {
    const result = assessIncidentHistoryRisk(
      [{ reportedAt: "2026-02-04T12:00:00Z", status: "confirmed" }],
      NOW,
    );

    expect(result.level).toBe("low");
    expect(result.daysSinceLastIncident).toBe(180);
  });

  it("returns watch risk for one recent incident", () => {
    const result = assessIncidentHistoryRisk(
      [{ reportedAt: "2026-07-24T12:00:00Z", status: "reported" }],
      NOW,
    );

    expect(result.level).toBe("watch");
    expect(result.recentCount).toBe(1);
  });

  it("returns high risk for two incidents within 180 days", () => {
    const result = assessIncidentHistoryRisk(
      [
        { reportedAt: "2026-07-24T12:00:00Z", status: "reported" },
        { reportedAt: "2026-05-01T12:00:00Z", status: "confirmed" },
      ],
      NOW,
    );

    expect(result.level).toBe("high");
    expect(result.recentCount).toBe(2);
  });

  it("returns high risk for three operational incidents in the visible history", () => {
    const result = assessIncidentHistoryRisk(
      [
        { reportedAt: "2026-01-01T12:00:00Z", status: "confirmed" },
        { reportedAt: "2025-12-01T12:00:00Z", status: "confirmed" },
        { reportedAt: "2025-11-01T12:00:00Z", status: "confirmed" },
      ],
      NOW,
    );

    expect(result.level).toBe("high");
  });
});
