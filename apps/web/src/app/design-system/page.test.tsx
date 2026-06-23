import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import DesignSystemPage from "./page";

describe("DesignSystemPage", () => {
  it("expone el catálogo de fundamentos visuales", () => {
    renderWithIntl(<DesignSystemPage />);

    expect(
      screen.getByRole("heading", { level: 1, name: "Lenguaje visual de Reserly" }),
    ).toBeVisible();
    expect(screen.getByRole("heading", { level: 2, name: "Paleta semántica" })).toBeVisible();
    expect(screen.getByRole("heading", { level: 2, name: "Tipografía" })).toBeVisible();
    expect(screen.getByRole("heading", { level: 2, name: "Estados" })).toBeVisible();
    expect(screen.getByRole("heading", { level: 2, name: "Iconografía" })).toBeVisible();
    expect(screen.getByRole("alert")).toHaveTextContent("Ejemplo de error");
    expect(screen.getByText("Disponible")).toBeVisible();
  });
});
