import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

import {
  PageContainer,
  PageHeading,
  PublicShell,
  ResponsiveGrid,
  Surface,
} from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";

const foundations = [
  {
    title: "Web pública",
    description: "Cabecera ligera, contenido fluido y navegación inferior en móvil.",
  },
  {
    title: "Panel del local",
    description: "Sidebar persistente en escritorio y accesos esenciales en móvil.",
  },
  {
    title: "Contenido reutilizable",
    description: "Contenedores, superficies, encabezados y grids con contratos comunes.",
  },
] as const;

/**
 * Vista de arranque del sistema de layout. No sustituye a la pantalla funcional
 * de inicio, que se implementará junto al buscador y sus datos.
 */
export default function HomePage() {
  return (
    <PublicShell>
      <PageContainer>
        <Stack spacing={{ xs: 4, md: 6 }}>
          <PageHeading
            actions={
              <Button
                component={NavigationLink}
                fullWidth
                href="/panel-preview"
                variant="contained"
              >
                Ver panel responsive
              </Button>
            }
            eyebrow="BASE DE INTERFAZ"
            summary="Una estructura común para construir las experiencias públicas y privadas sin duplicar navegación ni reglas responsive."
            title="Reserly ya tiene una base adaptable"
          />
          <ResponsiveGrid>
            {foundations.map((foundation) => (
              <Surface component="article" key={foundation.title}>
                <Typography component="h2" sx={{ fontSize: "1.125rem", fontWeight: 700 }}>
                  {foundation.title}
                </Typography>
                <Typography sx={{ color: "text.secondary", mt: 1 }}>
                  {foundation.description}
                </Typography>
              </Surface>
            ))}
          </ResponsiveGrid>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}
