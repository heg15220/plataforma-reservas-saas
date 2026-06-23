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
        gap: 2,
        justifyContent: "space-between",
      }}
    >
      <Box sx={{ maxWidth: 760 }}>
        {eyebrow && (
          <Typography
            component="p"
            sx={{ color: "primary.main", fontSize: "0.75rem", fontWeight: 700, mb: 1 }}
          >
            {eyebrow}
          </Typography>
        )}
        <Typography
          component="h1"
          sx={{
            fontSize: { xs: "1.75rem", md: "2.25rem" },
            fontWeight: 750,
            letterSpacing: "-0.035em",
            lineHeight: 1.15,
          }}
        >
          {title}
        </Typography>
        {summary && (
          <Typography
            component="p"
            sx={{ color: "text.secondary", fontSize: { xs: "0.95rem", md: "1rem" }, mt: 1.5 }}
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
