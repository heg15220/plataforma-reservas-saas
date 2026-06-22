import { describe, expect, it } from "vitest";

import { parseWebEnvironment } from "./environment";

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
});
