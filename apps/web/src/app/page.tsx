import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { LayoutDashboard, Palette, PanelsTopLeft, type LucideIcon } from "lucide-react";

import {
  PageContainer,
  PageHeading,
  PublicShell,
  ResponsiveGrid,
  Surface,
} from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip } from "@/components/visual";

const foundations = [
  {
    description: "Cabecera ligera, contenido fluido y navegación inferior en móvil.",
    icon: PanelsTopLeft,
    status: "Responsive",
    title: "Web pública",
  },
  {
    description: "Sidebar persistente en escritorio y accesos esenciales en móvil.",
    icon: LayoutDashboard,
    status: "Adaptable",
    title: "Panel del local",
  },
  {
    description: "Tokens, estados, iconos y componentes con contratos comunes.",
    icon: Palette,
    status: "Consistente",
    title: "Sistema visual",
  },
] satisfies ReadonlyArray<{
  description: string;
  icon: LucideIcon;
  status: string;
  title: string;
}>;

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
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <Button component={NavigationLink} href="/design-system" variant="outlined">
                  Ver sistema visual
                </Button>
                <Button component={NavigationLink} href="/panel-preview" variant="contained">
                  Ver panel responsive
                </Button>
              </Stack>
            }
            eyebrow="BASE DE PRODUCTO"
            summary="Una estructura común y un lenguaje visual accesible para construir experiencias públicas y privadas coherentes."
            title="Reserly ya tiene una base visual"
          />
          <ResponsiveGrid>
            {foundations.map((foundation) => {
              const Icon = foundation.icon;
              return (
                <Surface component="article" key={foundation.title}>
                  <Stack
                    direction="row"
                    sx={{
                      alignItems: "center",
                      color: "primary.main",
                      justifyContent: "space-between",
                    }}
                  >
                    <Icon aria-hidden="true" size={22} strokeWidth={1.9} />
                    <StatusChip label={foundation.status} tone="info" />
                  </Stack>
                  <Typography component="h2" variant="h2" sx={{ mt: 5 }}>
                    {foundation.title}
                  </Typography>
                  <Typography sx={{ color: "text.secondary", mt: 2 }}>
                    {foundation.description}
                  </Typography>
                </Surface>
              );
            })}
          </ResponsiveGrid>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}
