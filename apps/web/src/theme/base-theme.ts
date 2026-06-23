"use client";

import { createTheme } from "@mui/material/styles";

/**
 * Tema estructural provisional de la fase 0.
 *
 * La tarea 0.8 ampliará este contrato con tokens semánticos, estados, tipografía
 * completa e iconografía. Aquí solo se fijan decisiones necesarias para que los
 * shells y componentes base sean coherentes y accesibles.
 */
export const baseTheme = createTheme({
  cssVariables: true,
  typography: {
    fontFamily: 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButtonBase: {
      defaultProps: {
        disableRipple: true,
      },
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          minHeight: 44,
          textTransform: "none",
        },
      },
    },
  },
});
