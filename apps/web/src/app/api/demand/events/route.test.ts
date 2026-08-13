import { NextRequest } from "next/server";
import { afterEach, describe, expect, it, vi } from "vitest";

import { POST } from "./route";

describe("demand events route", () => {
  afterEach(() => vi.unstubAllEnvs());

  it("adds the server-only credential and forwards the body", async () => {
    vi.stubEnv("NEXT_PUBLIC_APP_ENV", "test");
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
    vi.stubEnv("RESERLY_API_INTERNAL_URL", "http://api:8080");
    vi.stubEnv(
      "RESERLY_DEMAND_INGESTION_SERVICE_TOKEN",
      "test-demand-token-at-least-32-characters",
    );
    const upstream = vi
      .fn()
      .mockResolvedValue(new Response('{"acceptedCount":1}', { status: 200 }));
    vi.stubGlobal("fetch", upstream);
    const body = '{"events":[]}';

    const response = await POST(
      new NextRequest("http://localhost/api/demand/events", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body,
      }),
    );

    expect(response.status).toBe(200);
    const [, init] = upstream.mock.calls[0] as [URL, RequestInit];
    expect(new Headers(init.headers).get("X-Reserly-Service-Token")).toBe(
      "test-demand-token-at-least-32-characters",
    );
    expect(init.body).toBe(body);
  });

  it("fails closed when the server credential is absent", async () => {
    vi.stubEnv("RESERLY_DEMAND_INGESTION_SERVICE_TOKEN", "");
    const response = await POST(
      new NextRequest("http://localhost/api/demand/events", {
        method: "POST",
        body: '{"events":[]}',
      }),
    );
    expect(response.status).toBe(503);
    expect(await response.json()).toEqual({ error: "EVENT_INGESTION_UNAVAILABLE" });
  });
});
