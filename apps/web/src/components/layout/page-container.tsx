import Container from "@mui/material/Container";
import type { ReactNode } from "react";

export interface PageContainerProps {
  children: ReactNode;
  compact?: boolean;
}

/**
 * Limita y centra el contenido con márgenes fluidos para móvil y escritorio.
 */
export function PageContainer({ children, compact = false }: PageContainerProps) {
  return (
    <Container
      component="div"
      maxWidth={false}
      sx={{
        maxWidth: compact ? 1120 : 1440,
        px: { xs: 4, sm: 6, lg: 8 },
        width: "100%",
      }}
    >
      {children}
    </Container>
  );
}
