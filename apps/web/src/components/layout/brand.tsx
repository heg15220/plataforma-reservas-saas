import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import { CalendarCheck2 } from "lucide-react";

export interface BrandProps {
  compact?: boolean;
  inverse?: boolean;
}

/**
 * Identidad compacta de Reserly para cabeceras y navegación.
 *
 * El isotipo combina calendario y confirmación sin depender de texto pequeño.
 * La variante compacta conserva el nombre accesible mediante `aria-label`.
 */
export function Brand({ compact = false, inverse = false }: BrandProps) {
  const foreground = inverse ? "common.white" : "text.primary";

  return (
    <Box
      aria-label="Reserly"
      sx={{ alignItems: "center", color: foreground, display: "inline-flex", gap: 1 }}
    >
      <Box
        aria-hidden="true"
        sx={{
          alignItems: "center",
          bgcolor: inverse ? "common.white" : "primary.main",
          borderRadius: 1.5,
          color: inverse ? "primary.main" : "common.white",
          display: "inline-flex",
          height: 32,
          justifyContent: "center",
          width: 32,
        }}
      >
        <CalendarCheck2 aria-hidden="true" size={19} strokeWidth={2.2} />
      </Box>
      {!compact && (
        <Typography component="span" sx={{ fontSize: "1.125rem", fontWeight: 700 }}>
          Reserly
        </Typography>
      )}
    </Box>
  );
}
