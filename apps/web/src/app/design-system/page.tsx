import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import {
  CalendarCheck2,
  CircleAlert,
  Clock3,
  Heart,
  MapPin,
  Search,
  ShieldCheck,
} from "lucide-react";
import type { Metadata } from "next";

import {
  PageContainer,
  PageHeading,
  PublicShell,
  ResponsiveGrid,
  Surface,
} from "@/components/layout";
import { NavigationLink } from "@/components/navigation-link";
import { StatusChip } from "@/components/visual";
import { visualTokens } from "@/theme/visual-tokens";

const palette = [
  { color: visualTokens.color.brand.primary, label: "Acción principal" },
  { color: visualTokens.color.brand.primarySoft, label: "Selección suave" },
  { color: visualTokens.color.status.success, label: "Éxito" },
  { color: visualTokens.color.status.warning, label: "Advertencia" },
  { color: visualTokens.color.status.danger, label: "Peligro" },
  { color: visualTokens.color.status.neutral, label: "Neutral" },
] as const;

const interfaceIcons = [
  { icon: Search, label: "Buscar" },
  { icon: MapPin, label: "Ubicación" },
  { icon: CalendarCheck2, label: "Reserva" },
  { icon: Heart, label: "Favorito" },
  { icon: ShieldCheck, label: "Verificación" },
  { icon: Clock3, label: "Horario" },
] as const;

export const metadata: Metadata = {
  title: "Sistema visual",
  robots: {
    index: false,
    follow: false,
  },
};

/**
 * Catálogo vivo de los fundamentos visuales compartidos por Reserly.
 *
 * No contiene datos de negocio. Su objetivo es permitir revisar tokens,
 * tipografía, controles, estados e iconografía en un entorno real de la app.
 */
export default function DesignSystemPage() {
  return (
    <PublicShell currentPath="">
      <PageContainer>
        <Stack spacing={{ xs: 8, md: 12 }}>
          <PageHeading
            actions={
              <Button component={NavigationLink} href="/" variant="outlined">
                Volver al inicio
              </Button>
            }
            eyebrow="SISTEMA VISUAL"
            summary="Fundamentos accesibles y reutilizables para mantener coherencia entre la web pública, el panel del local y la administración."
            title="Lenguaje visual de Reserly"
          />

          <Surface>
            <Typography component="h2" variant="h2">
              Paleta semántica
            </Typography>
            <Typography sx={{ color: "text.secondary", mt: 2 }}>
              Los colores expresan función. Los estados nunca dependen únicamente del color.
            </Typography>
            <ResponsiveGrid minColumnWidth={150}>
              {palette.map((token) => (
                <Box key={token.label} sx={{ mt: 4 }}>
                  <Box
                    aria-label={`${token.label}: ${token.color}`}
                    sx={{
                      bgcolor: token.color,
                      border: 1,
                      borderColor: "divider",
                      borderRadius: 2,
                      height: 72,
                    }}
                  />
                  <Typography sx={{ fontWeight: 600, mt: 2 }}>{token.label}</Typography>
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    {token.color}
                  </Typography>
                </Box>
              ))}
            </ResponsiveGrid>
          </Surface>

          <ResponsiveGrid minColumnWidth={280}>
            <Surface>
              <Typography component="h2" variant="h2">
                Tipografía
              </Typography>
              <Stack spacing={4} sx={{ mt: 4 }}>
                <Typography variant="h1">Título principal</Typography>
                <Typography variant="h2">Título de sección</Typography>
                <Typography variant="h3">Título de componente</Typography>
                <Typography>
                  Texto de lectura para explicar disponibilidad, reservas y siguientes acciones.
                </Typography>
                <Typography variant="body2" sx={{ color: "text.secondary" }}>
                  Metadatos y ayuda secundaria.
                </Typography>
              </Stack>
            </Surface>

            <Surface>
              <Typography component="h2" variant="h2">
                Acciones y campos
              </Typography>
              <Stack spacing={4} sx={{ mt: 4 }}>
                <Button variant="contained">Acción principal</Button>
                <Button variant="outlined">Acción secundaria</Button>
                <Button disabled variant="contained">
                  Acción deshabilitada
                </Button>
                <TextField
                  helperText="La etiqueta permanece visible durante la escritura."
                  label="Buscar un local"
                  placeholder="Nombre, servicio o categoría"
                />
              </Stack>
            </Surface>
          </ResponsiveGrid>

          <Surface>
            <Typography component="h2" variant="h2">
              Estados
            </Typography>
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 3, mt: 4 }}>
              <StatusChip label="Disponible" tone="success" />
              <StatusChip label="Pendiente" tone="warning" />
              <StatusChip label="Restringida" tone="danger" />
              <StatusChip label="Cerrado" tone="neutral" />
              <StatusChip label="Información" tone="info" />
            </Box>
            <Box
              role="alert"
              sx={{
                alignItems: "flex-start",
                bgcolor: visualTokens.color.status.dangerSoft,
                borderRadius: 2,
                color: visualTokens.color.status.dangerText,
                display: "flex",
                gap: 3,
                mt: 5,
                p: 4,
              }}
            >
              <CircleAlert aria-hidden="true" size={20} />
              <Typography>
                Ejemplo de error con icono, texto explícito y contraste independiente del color.
              </Typography>
            </Box>
          </Surface>

          <Surface>
            <Typography component="h2" variant="h2">
              Iconografía
            </Typography>
            <Typography sx={{ color: "text.secondary", mt: 2 }}>
              Lucide aporta iconos lineales de trazo coherente. Los iconos sin texto reciben nombre
              accesible; los decorativos se ocultan a tecnologías de asistencia.
            </Typography>
            <ResponsiveGrid minColumnWidth={120}>
              {interfaceIcons.map((item) => {
                const Icon = item.icon;
                return (
                  <Stack key={item.label} spacing={2} sx={{ alignItems: "center", mt: 5 }}>
                    <Box
                      sx={{
                        alignItems: "center",
                        bgcolor: "primary.light",
                        borderRadius: 3,
                        color: "primary.main",
                        display: "flex",
                        height: 48,
                        justifyContent: "center",
                        width: 48,
                      }}
                    >
                      <Icon aria-hidden="true" size={22} strokeWidth={1.9} />
                    </Box>
                    <Typography sx={{ fontWeight: 600 }}>{item.label}</Typography>
                  </Stack>
                );
              })}
            </ResponsiveGrid>
          </Surface>
        </Stack>
      </PageContainer>
    </PublicShell>
  );
}
