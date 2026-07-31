import Container from "@mui/material/Container";
import type { SxProps, Theme } from "@mui/material/styles";
import type { ReactNode } from "react";

export interface PageContainerProps {
  children: ReactNode;
  compact?: boolean;
  maxWidth?: "sm" | "md" | "lg" | "xl";
  sx?: SxProps<Theme>;
}

/**
 * Limita y centra el contenido con márgenes fluidos para móvil y escritorio.
 */
export function PageContainer({ children, compact = false, maxWidth, sx }: PageContainerProps) {
  return (
    <Container
      component="div"
      maxWidth={false}
      sx={{
        boxSizing: "border-box",
        maxWidth:
          maxWidth === "sm"
            ? 600
            : maxWidth === "md"
              ? 900
              : maxWidth === "lg"
                ? 1200
                : maxWidth === "xl"
                  ? 1440
                  : compact
                    ? 1120
                    : 1440,
        px: { xs: 4, sm: 6, lg: 8 },
        width: "auto",
        ...sx,
      }}
    >
      {children}
    </Container>
  );
}
