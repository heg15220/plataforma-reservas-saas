import Paper from "@mui/material/Paper";
import type { ReactNode } from "react";

import { visualTokens } from "@/theme/visual-tokens";

export interface SurfaceProps {
  children: ReactNode;
  component?: "article" | "aside" | "section";
  padded?: boolean;
}

/**
 * Superficie neutral para tarjetas, formularios y bloques de información.
 */
export function Surface({ children, component = "section", padded = true }: SurfaceProps) {
  return (
    <Paper
      component={component}
      elevation={0}
      sx={{
        border: 1,
        borderColor: "divider",
        borderRadius: `${visualTokens.radius.card}px`,
        minWidth: 0,
        p: padded ? { xs: 4, md: 6 } : 0,
      }}
    >
      {children}
    </Paper>
  );
}
