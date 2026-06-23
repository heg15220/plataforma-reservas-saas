import Box from "@mui/material/Box";
import type { ReactNode } from "react";

export interface ResponsiveGridProps {
  children: ReactNode;
  minColumnWidth?: number;
}

/**
 * Grid fluido que evita breakpoints específicos en listados de tarjetas.
 */
export function ResponsiveGrid({ children, minColumnWidth = 240 }: ResponsiveGridProps) {
  return (
    <Box
      sx={{
        display: "grid",
        gap: { xs: 2, md: 3 },
        gridTemplateColumns: `repeat(auto-fit, minmax(min(100%, ${minColumnWidth}px), 1fr))`,
      }}
    >
      {children}
    </Box>
  );
}
