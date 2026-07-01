import { describe, expect, it } from "vitest";

import { parseVenueLoginForm } from "./venue-login-schema";

function loginFormData(email = " local@example.com ", password = "correct-horse-battery") {
  const formData = new FormData();
  formData.set("email", email);
  formData.set("password", password);
  return formData;
}

describe("parseVenueLoginForm", () => {
  it("normaliza el email sin modificar la contraseña", () => {
    expect(parseVenueLoginForm(loginFormData())).toEqual({
      success: true,
      payload: {
        email: "local@example.com",
        password: "correct-horse-battery",
      },
    });
  });

  it("clasifica campos vacíos e email inválido", () => {
    expect(parseVenueLoginForm(loginFormData("", ""))).toEqual({
      success: false,
      errors: { email: "required", password: "required" },
    });
    expect(parseVenueLoginForm(loginFormData("incorrecto"))).toEqual({
      success: false,
      errors: { email: "email" },
    });
  });

  it("rechaza contraseñas que superan el límite BCrypt en UTF-8", () => {
    expect(parseVenueLoginForm(loginFormData("local@example.com", "á".repeat(37)))).toEqual({
      success: false,
      errors: { password: "passwordBytes" },
    });
  });
});
