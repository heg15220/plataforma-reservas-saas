import { afterEach, describe, expect, it, vi } from "vitest";

import { loadWebEnvironment, parseWebEnvironment } from "./environment";

afterEach(() => vi.unstubAllEnvs());

describe("parseWebEnvironment", () => {
  it("acepta URLs HTTP locales y usa la URL pública como fallback interno", () => {
    const environment = parseWebEnvironment({
      NEXT_PUBLIC_APP_ENV: "local",
      NEXT_PUBLIC_API_BASE_URL: "http://localhost:8080",
    });

    expect(environment).toEqual({
      appEnvironment: "local",
      publicApiBaseUrl: "http://localhost:8080",
      internalApiBaseUrl: "http://localhost:8080",
    });
  });

  it("mantiene la URL interna fuera del contrato público", () => {
    const environment = parseWebEnvironment({
      NEXT_PUBLIC_APP_ENV: "staging",
      NEXT_PUBLIC_API_BASE_URL: "https://api.staging.reserly.example",
      RESERLY_API_INTERNAL_URL: "http://reserly-api:8080",
    });

    expect(environment.internalApiBaseUrl).toBe("http://reserly-api:8080");
  });

  it("rechaza HTTP público en staging", () => {
    expect(() =>
      parseWebEnvironment({
        NEXT_PUBLIC_APP_ENV: "staging",
        NEXT_PUBLIC_API_BASE_URL: "http://api.staging.reserly.example",
      }),
    ).toThrow("NEXT_PUBLIC_API_BASE_URL debe usar HTTPS fuera de local y test.");
  });

  it("rechaza variables obligatorias ausentes", () => {
    expect(() => parseWebEnvironment({})).toThrow();
  });

  it("lee las variables públicas mediante el contrato compatible con bundles cliente", () => {
    vi.stubEnv("NEXT_PUBLIC_APP_ENV", "test");
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://public.test");
    vi.stubEnv("RESERLY_API_INTERNAL_URL", "http://internal.test");

    expect(loadWebEnvironment()).toEqual({
      appEnvironment: "test",
      publicApiBaseUrl: "http://public.test",
      internalApiBaseUrl: "http://public.test",
    });
  });
});
