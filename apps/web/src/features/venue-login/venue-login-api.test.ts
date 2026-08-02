import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { loginVenue, logoutVenue } from "./venue-login-api";

const payload = {
  email: "local@example.com",
  password: "correct-horse-battery",
};

const validResponse = {
  userId: "7ad3a532-86da-46f6-9cf5-c59107f48912",
  accountType: "venue_business",
  preferredLocale: "es",
  emailVerified: false,
  sessionExpiresAt: "2026-07-01T22:00:00Z",
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("loginVenue", () => {
  it("envía credenciales con cookies y valida los metadatos de sesión", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(validResponse), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(loginVenue(payload)).resolves.toEqual(validResponse);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/login",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        body: JSON.stringify(payload),
      }),
    );
  });

  it.each([
    [400, "invalid"],
    [401, "invalid"],
    [429, "rateLimited"],
    [503, "unavailable"],
  ] as const)("reduce HTTP %i a la categoría segura %s", async (status, kind) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status })));

    await expect(loginVenue(payload)).rejects.toMatchObject({ kind });
  });

  it("rechaza respuestas inesperadas sin confiar en metadatos alterados", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ ...validResponse, accountType: "admin" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(loginVenue(payload)).rejects.toMatchObject({ kind: "unavailable" });
  });

  it("convierte fallos de red en un error no sensible", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("detalle interno")));

    await expect(loginVenue(payload)).rejects.toMatchObject({ kind: "unavailable" });
  });
});

describe("logoutVenue", () => {
  it("revoca la sesión enviando la cookie HttpOnly", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(logoutVenue()).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/logout",
      expect.objectContaining({ method: "POST", credentials: "include" }),
    );
  });

  it.each([401, 503])("no simula un cierre correcto ante HTTP %i", async (status) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status })));

    await expect(logoutVenue()).rejects.toMatchObject({ kind: "unavailable" });
  });
});
