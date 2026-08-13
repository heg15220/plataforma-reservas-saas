"use client";

import { AppRouterCacheProvider } from "@mui/material-nextjs/v16-appRouter";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import { NextIntlClientProvider } from "next-intl";
import type { ReactNode } from "react";

import { defaultTimeZone, type Messages, type SupportedLocale } from "@/i18n/config";
import { DemandConsentManager } from "@/features/privacy/demand-consent-manager";
import { baseTheme } from "@/theme/base-theme";

export interface AppProvidersProps {
  children: ReactNode;
  locale: SupportedLocale;
  messages: Messages;
}

/**
 * Registra el cache SSR de MUI, el tema base y la normalización global.
 *
 * @param children árbol activo del App Router
 */
export function AppProviders({ children, locale, messages }: Readonly<AppProvidersProps>) {
  return (
    <AppRouterCacheProvider options={{ enableCssLayer: true }}>
      <NextIntlClientProvider locale={locale} messages={messages} timeZone={defaultTimeZone}>
        <ThemeProvider theme={baseTheme}>
          <CssBaseline />
          {children}
          <DemandConsentManager />
        </ThemeProvider>
      </NextIntlClientProvider>
    </AppRouterCacheProvider>
  );
}
