import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";

export interface PageHeadingProps {
  actions?: ReactNode;
  eyebrow?: string;
  summary?: string;
  title: string;
}

/**
 * Encabezado responsive de página con acción principal opcional.
 */
export function PageHeading({ actions, eyebrow, summary, title }: PageHeadingProps) {
  return (
    <Box
      component="header"
      sx={{
        alignItems: { md: "flex-end" },
        display: "flex",
        flexDirection: { xs: "column", md: "row" },
        gap: 4,
        justifyContent: "space-between",
      }}
    >
      <Box sx={{ maxWidth: 760 }}>
        {eyebrow && (
          <Typography component="p" variant="overline" sx={{ color: "primary.main", mb: 2 }}>
            {eyebrow}
          </Typography>
        )}
        <Typography component="h1" variant="h1">
          {title}
        </Typography>
        {summary && (
          <Typography
            component="p"
            sx={{ color: "text.secondary", fontSize: { xs: "0.875rem", md: "1rem" }, mt: 3 }}
          >
            {summary}
          </Typography>
        )}
      </Box>
      {actions && (
        <Box sx={{ alignSelf: { xs: "stretch", md: "auto" }, flexShrink: 0 }}>{actions}</Box>
      )}
    </Box>
  );
}
