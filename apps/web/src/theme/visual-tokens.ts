/**
 * Tokens visuales semánticos de Reserly.
 *
 * Los componentes deben consumir estos nombres o los alias del tema MUI en vez
 * de declarar colores, radios o sombras de producto de forma aislada.
 */
export const visualTokens = {
  color: {
    brand: {
      primary: "#075FE4",
      primaryHover: "#064FC0",
      primarySoft: "#EAF2FF",
    },
    text: {
      primary: "#111827",
      secondary: "#5F6B7A",
      inverse: "#FFFFFF",
    },
    surface: {
      page: "#F7F9FC",
      card: "#FFFFFF",
      raised: "#FFFFFF",
      inverse: "#172033",
    },
    border: {
      default: "#DFE5EE",
      strong: "#B8C2D1",
    },
    status: {
      success: "#0AA968",
      successText: "#067647",
      successSoft: "#E7F8EF",
      warning: "#F59E0B",
      warningText: "#8A4B00",
      warningSoft: "#FFF4DB",
      danger: "#E53935",
      dangerText: "#B42318",
      dangerSoft: "#FDECEC",
      neutral: "#8A94A3",
      neutralText: "#475467",
      neutralSoft: "#F2F4F7",
      infoText: "#075FE4",
      infoSoft: "#EAF2FF",
    },
  },
  radius: {
    control: 8,
    card: 12,
    panel: 16,
    round: 999,
  },
  shadow: {
    floating: "0 12px 32px rgba(17, 24, 39, 0.12)",
    card: "0 4px 16px rgba(17, 24, 39, 0.06)",
  },
  typography: {
    family: 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    weight: {
      regular: 400,
      medium: 500,
      semibold: 600,
      bold: 700,
    },
  },
} as const;
