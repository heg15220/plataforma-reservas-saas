import { render, type RenderOptions } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement, ReactNode } from "react";

import enMessages from "../../locales/en.json";
import esMessages from "../../locales/es.json";

type TestLocale = "en" | "es";

function IntlTestProvider({
  children,
  locale,
}: Readonly<{ children: ReactNode; locale: TestLocale }>) {
  return (
    <NextIntlClientProvider locale={locale} messages={locale === "es" ? esMessages : enMessages}>
      {children}
    </NextIntlClientProvider>
  );
}

export function renderWithIntl(ui: ReactElement, options?: Omit<RenderOptions, "wrapper">) {
  return renderWithLocale(ui, "es", options);
}

/** Renderiza componentes con el catálogo real solicitado para cobertura bilingüe focalizada. */
export function renderWithLocale(
  ui: ReactElement,
  locale: TestLocale,
  options?: Omit<RenderOptions, "wrapper">,
) {
  const Wrapper = ({ children }: Readonly<{ children: ReactNode }>) => (
    <IntlTestProvider locale={locale}>{children}</IntlTestProvider>
  );
  return render(ui, { wrapper: Wrapper, ...options });
}
