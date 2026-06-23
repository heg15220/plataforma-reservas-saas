import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { StatusChip } from "./status-chip";

describe("StatusChip", () => {
  it.each([
    ["success", "Confirmada"],
    ["warning", "Pendiente"],
    ["danger", "Restringida"],
    ["neutral", "Cerrado"],
    ["info", "Información"],
  ] as const)("representa el estado %s con una etiqueta accesible", (tone, label) => {
    render(<StatusChip label={label} tone={tone} />);

    expect(screen.getByText(label)).toBeVisible();
    expect(screen.getByText(label).closest(".MuiChip-root")?.querySelector("svg")).toHaveAttribute(
      "aria-hidden",
      "true",
    );
  });
});
