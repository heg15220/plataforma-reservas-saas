import Paper from "@mui/material/Paper";
import type { SxProps, Theme } from "@mui/material/styles";
import type { ReactNode } from "react";

import { visualTokens } from "@/theme/visual-tokens";

export interface SurfaceProps {
  "aria-label"?: string;
  children: ReactNode;
  component?: "article" | "aside" | "main" | "section";
  padding?: "none" | "sm" | "md" | "lg";
  padded?: boolean;
  sx?: SxProps<Theme>;
  tone?: "default" | "muted";
}

/**
 * Superficie neutral para tarjetas, formularios y bloques de información.
 */
export function Surface({
  "aria-label": ariaLabel,
  children,
  component = "section",
  padding,
  padded = true,
  sx,
  tone = "default",
}: SurfaceProps) {
  const resolvedPadding =
    padding === "none"
      ? 0
      : padding === "sm"
        ? 2
        : padding === "md"
          ? { xs: 2, md: 2.5 }
          : padding === "lg"
            ? { xs: 2.5, md: 3 }
            : padded
              ? { xs: 4, md: 5 }
              : 0;
  return (
    <Paper
      aria-label={ariaLabel}
      component={component}
      elevation={0}
      sx={{
        bgcolor: tone === "muted" ? "grey.50" : "background.paper",
        border: 1,
        borderColor: "divider",
        borderRadius: `${visualTokens.radius.card}px`,
        boxShadow: visualTokens.shadow.card,
        minWidth: 0,
        p: resolvedPadding,
        ...sx,
      }}
    >
      {children}
    </Paper>
  );
}
