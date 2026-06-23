import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import HomePage from "./page";

describe("HomePage", () => {
  it("muestra el layout público y sus regiones principales", () => {
    render(<HomePage />);

    expect(
      screen.getByRole("heading", { level: 1, name: "Reserly ya tiene una base visual" }),
    ).toBeVisible();
    expect(screen.getByRole("navigation", { name: "Navegación pública principal" })).toBeVisible();
    expect(
      screen.getByRole("navigation", { name: "Navegación pública móvil" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ver panel responsive" })).toHaveAttribute(
      "href",
      "/panel-preview",
    );
    expect(screen.getByRole("link", { name: "Ver sistema visual" })).toHaveAttribute(
      "href",
      "/design-system",
    );
    expect(screen.getAllByRole("article")).toHaveLength(3);
  });
});
