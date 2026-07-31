/**
 * Tokens visuales semánticos de Reserly.
 *
 * Los componentes deben consumir estos nombres o los alias del tema MUI en vez
 * de declarar colores, radios o sombras de producto de forma aislada.
 */
export const visualTokens = {
  color: {
    brand: {
      primary: "#075CD6",
      primaryHover: "#064DB5",
      primarySoft: "#EAF2FF",
    },
    text: {
      primary: "#111C33",
      secondary: "#5B677A",
      inverse: "#FFFFFF",
    },
    surface: {
      page: "#F8FAFD",
      card: "#FFFFFF",
      raised: "#FFFFFF",
      inverse: "#10203A",
    },
    border: {
      default: "#E3E9F1",
      strong: "#C7D0DE",
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
    control: 7,
    card: 10,
    panel: 12,
    round: 999,
  },
  shadow: {
    floating: "0 18px 48px rgba(18, 39, 78, 0.14)",
    card: "0 3px 14px rgba(18, 39, 78, 0.055)",
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
