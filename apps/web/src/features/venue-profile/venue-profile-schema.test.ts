import { describe, expect, it } from "vitest";

import { parseVenueProfileForm } from "./venue-profile-schema";

function validForm() {
  const form = new FormData();
  form.set("name", "Casa Luz");
  form.set("categoryId", "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
  form.set("defaultLocale", "es");
  form.set("description_es", "Cocina de temporada");
  form.set("description_en", "Seasonal cuisine");
  return form;
}

describe("parseVenueProfileForm", () => {
  it("acepta identificadores UUID estables de los fixtures PostgreSQL", () => {
    const form = validForm();
    form.set("categoryId", "20000000-0000-0000-0000-000000000001");

    const result = parseVenueProfileForm(form);

    expect(result.success).toBe(true);
  });

  it("normaliza blancos, textos localizados y visibilidad de contacto", () => {
    const form = new FormData();
    form.set("name", " Casa Luz ");
    form.set("categoryId", "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    form.set("defaultLocale", "es");
    form.set("description_es", " Cocina de temporada ");
    form.set("description_en", " Seasonal cuisine ");
    form.set("contactEmail", "hola@casaluz.test");
    form.set("phone", "+34 910 000 000");
    form.set("country", "es");
    form.set("latitude", "40.416775");
    form.set("longitude", "-3.703790");
    form.set("showEmail", "on");

    const result = parseVenueProfileForm(form);

    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.payload.name).toBe("Casa Luz");
      expect(result.payload.country).toBe("ES");
      expect(result.payload.showEmail).toBe(true);
      expect(result.payload.showPhone).toBe(false);
      expect(result.payload.descriptionI18n?.values).toEqual({
        en: "Seasonal cuisine",
        es: "Cocina de temporada",
      });
    }
  });

  it("devuelve errores seguros para campos obligatorios o inválidos", () => {
    const form = new FormData();
    form.set("defaultLocale", "es");
    form.set("country", "España");
    form.set("latitude", "200");

    const result = parseVenueProfileForm(form);

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.errors.name).toBe("required");
      expect(result.errors.categoryId).toBe("invalid");
      expect(result.errors.country).toBe("invalid");
      expect(result.errors.latitude).toBe("invalid");
    }
  });
});
