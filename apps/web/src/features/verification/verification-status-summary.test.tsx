import { cleanup, render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { afterEach, describe, expect, it } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import enMessages from "../../../locales/en.json";
import { VerificationStatusSummary } from "./verification-status-summary";

afterEach(cleanup);

describe("VerificationStatusSummary", () => {
  it("presenta ambas barreras con texto y semántica localizada", () => {
    renderWithIntl(
      <VerificationStatusSummary businessStatus="pending_remote_check" emailVerified={false} />,
    );

    expect(screen.getByRole("heading", { name: "Estado de las comprobaciones" })).toBeVisible();
    expect(screen.getByText("Correo electrónico")).toBeVisible();
    expect(screen.getByText("Confirmación pendiente")).toBeVisible();
    expect(screen.getByText("Identidad empresarial")).toBeVisible();
    expect(screen.getByText("Comprobación en curso")).toBeVisible();
    expect(screen.queryByText("pending_remote_check")).not.toBeInTheDocument();
  });

  it("representa estados positivos y negativos sin depender solo del color", () => {
    const { rerender } = renderWithIntl(
      <VerificationStatusSummary businessStatus="verified" emailVerified />,
    );

    expect(screen.getByText("Correo confirmado")).toBeVisible();
    expect(screen.getByText("Negocio verificado")).toBeVisible();

    rerender(<VerificationStatusSummary businessStatus="rejected" emailVerified />);
    expect(screen.getByText("Verificación no aprobada")).toBeVisible();
    expect(
      screen.getByText(/Revisa la información indicada antes de solicitar una nueva comprobación/),
    ).toBeVisible();
  });

  it("renderiza el contrato inglés sin fallback a español ni códigos técnicos", () => {
    render(
      <NextIntlClientProvider locale="en" messages={enMessages}>
        <VerificationStatusSummary businessStatus="expired" emailVerified />
      </NextIntlClientProvider>,
    );

    expect(screen.getByRole("heading", { name: "Check status" })).toBeVisible();
    expect(screen.getByText("Email confirmed")).toBeVisible();
    expect(screen.getByText("Verification expired")).toBeVisible();
    expect(screen.queryByText("expired")).not.toBeInTheDocument();
    expect(screen.queryByText("Verificación caducada")).not.toBeInTheDocument();
  });
});
