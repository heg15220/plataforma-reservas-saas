import { describe, expect, it } from "vitest";

import {
  readSafeLocaleTag,
  resolveAcceptLanguageLocale,
  resolveEffectiveLocale,
  resolveLocaleTag,
  resolveSavedLocale,
} from "./locale-resolution";

describe("resolución de idioma", () => {
  it("acepta solo preferencias persistidas exactas y soportadas", () => {
    expect(resolveSavedLocale("es")).toBe("es");
    expect(resolveSavedLocale("EN")).toBe("en");
    expect(resolveSavedLocale("es-MX")).toBeUndefined();
    expect(resolveSavedLocale("fr")).toBeUndefined();
  });

  it("normaliza parámetros seguros a español o fallback inglés", () => {
    expect(resolveLocaleTag("es")).toBe("es");
    expect(resolveLocaleTag("es-MX")).toBe("es");
    expect(resolveLocaleTag("en-US")).toBe("en");
    expect(resolveLocaleTag("fr-FR")).toBe("en");
  });

  it("rechaza parámetros inseguros antes de resolver idioma", () => {
    expect(readSafeLocaleTag("es<script>")).toBeUndefined();
    expect(readSafeLocaleTag("../../es")).toBeUndefined();
    expect(readSafeLocaleTag("es-ES-extra-extra-extra-extra")).toBeUndefined();
    expect(resolveLocaleTag("es<script>")).toBeUndefined();
  });

  it("respeta la prioridad de preferencia guardada, parámetro, app, navegador y fallback", () => {
    expect(
      resolveEffectiveLocale({
        savedPreference: "en",
        explicitLocale: "es-MX",
        appLocale: "es-ES",
        acceptLanguage: "es-ES,es;q=0.9",
      }),
    ).toEqual({ locale: "en", source: "saved-preference" });

    expect(resolveEffectiveLocale({ explicitLocale: "es-AR" })).toEqual({
      locale: "es",
      source: "explicit-parameter",
    });

    expect(resolveEffectiveLocale({ appLocale: "es-ES" })).toEqual({
      locale: "es",
      source: "app-locale",
    });

    expect(resolveEffectiveLocale({ acceptLanguage: "es-ES,es;q=0.9,en;q=0.8" })).toEqual({
      locale: "es",
      source: "accept-language",
    });

    expect(resolveEffectiveLocale({})).toEqual({ locale: "en", source: "fallback" });
  });

  it("interpreta Accept-Language por calidad y orden", () => {
    expect(resolveAcceptLanguageLocale("es-ES,es;q=0.9,en;q=0.8")).toBe("es");
    expect(resolveAcceptLanguageLocale("en-US,en;q=0.9,es;q=0.8")).toBe("en");
    expect(resolveAcceptLanguageLocale("fr-FR,fr;q=0.9")).toBe("en");
    expect(resolveAcceptLanguageLocale("es;q=0,en;q=1")).toBe("en");
    expect(resolveAcceptLanguageLocale("bad_locale,es-MX;q=0.7")).toBe("es");
  });
});
