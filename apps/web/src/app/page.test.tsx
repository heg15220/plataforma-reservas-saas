import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import HomePage from "./page";

describe("HomePage", () => {
  it("muestra la identidad inicial de Reserly", () => {
    render(<HomePage />);

    expect(screen.getByRole("heading", { level: 1, name: "Reserly" })).toBeVisible();
    expect(screen.getByText("Booking made simple.")).toBeVisible();
  });
});
