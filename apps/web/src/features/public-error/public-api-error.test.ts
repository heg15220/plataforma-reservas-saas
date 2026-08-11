import { describe, expect, it } from "vitest";

import enMessages from "../../../locales/en.json";
import esMessages from "../../../locales/es.json";

import { PublicApiError } from "./public-api-error";

describe("PublicApiError", () => {
  it("propaga solo una clave i18n y el estado programático", () => {
    const error = new PublicApiError("PublicErrors.unavailable", 503);

    expect(error).toMatchObject({
      message: "PublicErrors.unavailable",
      messageKey: "PublicErrors.unavailable",
      status: 503,
    });
    expect(error.message).not.toMatch(/provider|response|503/i);
  });

  it("mantiene completo el catálogo público en español e inglés", () => {
    expect(Object.keys(esMessages.PublicErrors).sort()).toEqual(
      Object.keys(enMessages.PublicErrors).sort(),
    );
    expect(esMessages.PublicErrors.unavailable).toContain("Inténtalo");
    expect(enMessages.PublicErrors.unavailable).toContain("try again");
  });
});
