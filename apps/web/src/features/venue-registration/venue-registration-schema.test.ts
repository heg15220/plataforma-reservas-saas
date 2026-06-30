import { describe, expect, it } from "vitest";

import { parseVenueRegistrationForm } from "./venue-registration-schema";

function validFormData() {
  const formData = new FormData();
  formData.set("email", "  negocio@example.com ");
  formData.set("password", "correct-horse-battery");
  formData.set("taxCountry", " es ");
  formData.set("legalName", "  Ejemplo Reservas SL ");
  formData.set("taxIdentifier", " ES/B-12345674 ");
  formData.set("registeredAddress", "  Calle Mayor 1 ");
  formData.set("acceptsLegalTerms", "on");
  return formData;
}

describe("parseVenueRegistrationForm", () => {
  it("normaliza campos y construye el contrato de la API", () => {
    const result = parseVenueRegistrationForm(validFormData(), "es");

    expect(result).toEqual({
      success: true,
      payload: {
        account: {
          email: "negocio@example.com",
          password: "correct-horse-battery",
          preferredLocale: "es",
        },
        business: {
          taxCountry: "ES",
          legalName: "Ejemplo Reservas SL",
          taxIdentifier: "ES/B-12345674",
          registeredAddress: "Calle Mayor 1",
        },
        acceptsLegalTerms: true,
      },
    });
  });

  it("rechaza campos obligatorios, país y consentimiento inválidos", () => {
    const formData = validFormData();
    formData.set("email", "incorrecto");
    formData.set("taxCountry", "España");
    formData.delete("legalName");
    formData.delete("acceptsLegalTerms");

    const result = parseVenueRegistrationForm(formData, "es");

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.errors).toMatchObject({
        email: "email",
        taxCountry: "country",
        legalName: "required",
        acceptsLegalTerms: "legalTerms",
      });
    }
  });

  it("aplica el límite de 72 bytes además del límite de caracteres", () => {
    const formData = validFormData();
    formData.set("password", "á".repeat(40));

    const result = parseVenueRegistrationForm(formData, "es");

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.errors.password).toBe("passwordBytes");
    }
  });
});
