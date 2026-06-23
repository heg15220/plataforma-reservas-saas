"use client";

import { AppRouterCacheProvider } from "@mui/material-nextjs/v16-appRouter";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import type { ReactNode } from "react";

import { baseTheme } from "@/theme/base-theme";

/**
 * Registra el cache SSR de MUI, el tema base y la normalización global.
 *
 * @param children árbol activo del App Router
 */
export function AppProviders({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <AppRouterCacheProvider options={{ enableCssLayer: true }}>
      <ThemeProvider theme={baseTheme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </AppRouterCacheProvider>
  );
}
