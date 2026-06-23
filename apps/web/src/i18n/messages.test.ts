import { describe, expect, it } from "vitest";

import enMessages from "../../locales/en.json";
import esMessages from "../../locales/es.json";
import { defaultLocale, fallbackLocale, isSupportedLocale, supportedLocales } from "./config";

function flattenKeys(value: unknown, prefix = ""): string[] {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return [prefix];
  }

  return Object.entries(value).flatMap(([key, nestedValue]) =>
    flattenKeys(nestedValue, prefix ? `${prefix}.${key}` : key),
  );
}

describe("catálogos i18n", () => {
  it("declara los locales soportados y el fallback operativo", () => {
    expect(supportedLocales).toEqual(["es", "en"]);
    expect(defaultLocale).toBe("en");
    expect(fallbackLocale).toBe("en");
    expect(isSupportedLocale("es")).toBe(true);
    expect(isSupportedLocale("en")).toBe(true);
    expect(isSupportedLocale("fr")).toBe(false);
  });

  it("mantiene las mismas claves en español e inglés", () => {
    expect(flattenKeys(esMessages).sort()).toEqual(flattenKeys(enMessages).sort());
  });

  it("conserva caracteres españoles críticos en el catálogo base", () => {
    expect(esMessages.DesignSystem.palette.success).toBe("Éxito");
    expect(esMessages.Navigation.venue.more).toBe("Más");
    expect(esMessages.DesignSystem.icons.location).toBe("Ubicación");
    expect(esMessages.PanelPreview.cards.nextSlot.label).toBe("Próxima franja");
  });
});
