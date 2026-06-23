import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { Metadata } from "next";

import { PageHeading, ResponsiveGrid, Surface, VenueShell } from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";

const summaryCards = [
  { label: "Reservas de hoy", value: "—" },
  { label: "Próxima franja", value: "—" },
  { label: "Ocupación", value: "—" },
] as const;

export const metadata: Metadata = {
  title: "Preview del panel",
  robots: {
    index: false,
    follow: false,
  },
};

/**
 * Preview estructural del panel mientras los datos y casos de uso están pendientes.
 */
export default function PanelPreviewPage() {
  return (
    <VenueShell venueName="Vista de demostración">
      <Stack spacing={{ xs: 4, md: 5 }}>
        <PageHeading
          actions={
            <Button component={NavigationLink} fullWidth href="/" variant="outlined">
              Volver a la web pública
            </Button>
          }
          eyebrow="PANEL DEL LOCAL"
          summary="Los datos reales aparecerán cuando se implementen identidad, locales y reservas."
          title="Resumen"
        />
        <ResponsiveGrid minColumnWidth={220}>
          {summaryCards.map((card) => (
            <Surface component="article" key={card.label}>
              <Typography sx={{ color: "text.secondary", fontSize: "0.875rem" }}>
                {card.label}
              </Typography>
              <Typography
                aria-label={`${card.label}: sin datos`}
                sx={{ fontSize: "2rem", fontWeight: 750, mt: 1 }}
              >
                {card.value}
              </Typography>
            </Surface>
          ))}
        </ResponsiveGrid>
        <Surface>
          <Typography component="h2" sx={{ fontSize: "1.125rem", fontWeight: 700 }}>
            Estado vacío
          </Typography>
          <Typography sx={{ color: "text.secondary", mt: 1 }}>
            Este bloque demuestra cómo reservar espacio para estados de carga, vacío y error sin
            cambiar la estructura principal de la página.
          </Typography>
        </Surface>
      </Stack>
    </VenueShell>
  );
}
