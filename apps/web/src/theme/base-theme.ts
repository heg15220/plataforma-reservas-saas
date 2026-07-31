"use client";

import { createTheme } from "@mui/material/styles";

import { visualTokens } from "./visual-tokens";

/**
 * Tema visual global de Reserly.
 *
 * Traduce los tokens semánticos a contratos de Material UI y centraliza los
 * estados interactivos compartidos. Los componentes de producto deben preferir
 * alias como `primary.main`, `background.default` o `text.secondary`.
 */
export const baseTheme = createTheme({
  cssVariables: true,
  spacing: 4,
  palette: {
    mode: "light",
    primary: {
      main: visualTokens.color.brand.primary,
      dark: visualTokens.color.brand.primaryHover,
      light: visualTokens.color.brand.primarySoft,
      contrastText: visualTokens.color.text.inverse,
    },
    success: {
      main: visualTokens.color.status.success,
      dark: visualTokens.color.status.successText,
      light: visualTokens.color.status.successSoft,
    },
    warning: {
      main: visualTokens.color.status.warning,
      dark: visualTokens.color.status.warningText,
      light: visualTokens.color.status.warningSoft,
    },
    error: {
      main: visualTokens.color.status.danger,
      dark: visualTokens.color.status.dangerText,
      light: visualTokens.color.status.dangerSoft,
    },
    text: {
      primary: visualTokens.color.text.primary,
      secondary: visualTokens.color.text.secondary,
    },
    background: {
      default: visualTokens.color.surface.page,
      paper: visualTokens.color.surface.card,
    },
    divider: visualTokens.color.border.default,
  },
  typography: {
    fontFamily: visualTokens.typography.family,
    h1: {
      fontSize: "clamp(1.75rem, 1.4rem + 1.25vw, 2.5rem)",
      fontWeight: visualTokens.typography.weight.bold,
      letterSpacing: "-0.025em",
      lineHeight: 1.25,
    },
    h2: {
      fontSize: "1.125rem",
      fontWeight: visualTokens.typography.weight.semibold,
      letterSpacing: "-0.015em",
      lineHeight: 1.4,
    },
    h3: {
      fontSize: "1rem",
      fontWeight: visualTokens.typography.weight.semibold,
      lineHeight: 1.5,
    },
    body1: {
      fontSize: "0.8125rem",
      lineHeight: 1.5,
    },
    body2: {
      fontSize: "0.725rem",
      lineHeight: 1.5,
    },
    button: {
      fontSize: "0.75rem",
      fontWeight: visualTokens.typography.weight.semibold,
      lineHeight: 1.25,
      textTransform: "none",
    },
    overline: {
      fontSize: "0.75rem",
      fontWeight: visualTokens.typography.weight.bold,
      letterSpacing: "0.08em",
      lineHeight: 1.5,
    },
  },
  shape: {
    borderRadius: visualTokens.radius.control,
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: visualTokens.color.surface.page,
          color: visualTokens.color.text.primary,
          WebkitFontSmoothing: "antialiased",
        },
      },
    },
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
          borderRadius: visualTokens.radius.control,
          fontWeight: visualTokens.typography.weight.semibold,
          minHeight: 38,
          textTransform: "none",
          transition: "background-color 120ms ease, border-color 120ms ease, color 120ms ease",
          "&:focus-visible": {
            outline: `3px solid ${visualTokens.color.brand.primary}`,
            outlineOffset: 3,
          },
        },
        contained: {
          "&.MuiButton-containedPrimary:hover": {
            backgroundColor: visualTokens.color.brand.primaryHover,
          },
        },
        outlined: {
          borderColor: visualTokens.color.border.strong,
          "&:hover": {
            backgroundColor: visualTokens.color.brand.primarySoft,
            borderColor: visualTokens.color.brand.primary,
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: visualTokens.radius.round,
          fontWeight: visualTokens.typography.weight.semibold,
          minHeight: 28,
        },
        icon: {
          marginLeft: 8,
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: visualTokens.radius.control,
          minHeight: 40,
          "&:hover .MuiOutlinedInput-notchedOutline": {
            borderColor: visualTokens.color.border.strong,
          },
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
            borderWidth: 2,
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        rounded: {
          borderRadius: visualTokens.radius.card,
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: visualTokens.color.surface.inverse,
          borderRadius: visualTokens.radius.control,
          fontSize: "0.75rem",
        },
      },
    },
  },
});
