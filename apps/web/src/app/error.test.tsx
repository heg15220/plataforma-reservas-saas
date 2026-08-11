import { fireEvent, render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { describe, expect, it, vi } from "vitest";

import esMessages from "../../locales/es.json";

import AppError from "./error";

describe("AppError", () => {
  it("muestra el fallback i18n sin renderizar detalles externos", () => {
    const reset = vi.fn();
    render(
      <NextIntlClientProvider locale="es" messages={esMessages}>
        <AppError reset={reset} />
      </NextIntlClientProvider>,
    );

    expect(screen.getByText(esMessages.PublicErrors.title)).toBeVisible();
    expect(screen.getByText(esMessages.PublicErrors.unavailable)).toBeVisible();
    expect(screen.queryByText(/provider|response|stack|digest/i)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: esMessages.PublicErrors.retry }));
    expect(reset).toHaveBeenCalledOnce();
  });
});
