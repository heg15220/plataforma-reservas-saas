import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";

export interface BrandProps {
  compact?: boolean;
  inverse?: boolean;
}

/**
 * Identidad tipográfica provisional reutilizable en cabeceras y navegación.
 *
 * El recurso gráfico definitivo corresponde a la tarea 0.8.
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
          fontSize: "0.875rem",
          fontWeight: 800,
          height: 32,
          justifyContent: "center",
          width: 32,
        }}
      >
        R
      </Box>
      {!compact && (
        <Typography component="span" sx={{ fontSize: "1.125rem", fontWeight: 750 }}>
          Reserly
        </Typography>
      )}
    </Box>
  );
}
