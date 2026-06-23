import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Toolbar from "@mui/material/Toolbar";
import type { ReactNode } from "react";

import { NavigationLink } from "@/components/navigation-link";

import { Brand } from "./brand";
import { PageContainer } from "./page-container";

const publicNavigation = [
  { href: "/", label: "Inicio" },
  { href: "/explorar", label: "Explorar" },
  { href: "/reservas", label: "Reservas" },
  { href: "/favoritos", label: "Favoritos" },
  { href: "/perfil", label: "Perfil" },
] as const;

export interface PublicShellProps {
  children: ReactNode;
  currentPath?: string;
}

/**
 * Layout de la experiencia pública.
 *
 * En escritorio muestra cabecera horizontal; en móvil conserva acciones
 * esenciales en cabecera y ofrece navegación inferior táctil.
 */
export function PublicShell({ children, currentPath = "/" }: PublicShellProps) {
  return (
    <Box sx={{ bgcolor: "background.default", minHeight: "100dvh", pb: { xs: 9, md: 0 } }}>
      <Box className="skip-link" component="a" href="#main-content">
        Saltar al contenido
      </Box>
      <AppBar
        color="inherit"
        elevation={0}
        position="sticky"
        sx={{ borderBottom: 1, borderColor: "divider" }}
      >
        <PageContainer>
          <Toolbar disableGutters sx={{ gap: 2, minHeight: { xs: 64, md: 72 } }}>
            <NavigationLink aria-label="Ir al inicio de Reserly" className="unstyled-link" href="/">
              <Brand />
            </NavigationLink>
            <Box
              component="nav"
              aria-label="Navegación pública principal"
              sx={{ display: { xs: "none", md: "flex" }, gap: 0.5, ml: "auto" }}
            >
              {publicNavigation.slice(0, 3).map((item) => (
                <Button
                  aria-current={currentPath === item.href ? "page" : undefined}
                  color={currentPath === item.href ? "primary" : "inherit"}
                  component={NavigationLink}
                  href={item.href}
                  key={item.href}
                >
                  {item.label}
                </Button>
              ))}
            </Box>
            <Button
              component={NavigationLink}
              href="/acceso-local"
              size="small"
              sx={{ ml: { xs: "auto", md: 1 } }}
              variant="outlined"
            >
              Acceso local
            </Button>
          </Toolbar>
        </PageContainer>
      </AppBar>

      <Box component="main" id="main-content" sx={{ py: { xs: 3, md: 5 } }}>
        {children}
      </Box>

      <Paper
        component="nav"
        aria-label="Navegación pública móvil"
        elevation={8}
        square
        sx={{
          bottom: 0,
          display: { xs: "grid", md: "none" },
          gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
          left: 0,
          position: "fixed",
          right: 0,
          zIndex: "appBar",
        }}
      >
        {publicNavigation.map((item) => (
          <Button
            aria-current={currentPath === item.href ? "page" : undefined}
            color={currentPath === item.href ? "primary" : "inherit"}
            component={NavigationLink}
            href={item.href}
            key={item.href}
            size="small"
            sx={{
              borderRadius: 0,
              fontSize: "0.6875rem",
              minHeight: 64,
              minWidth: 0,
              px: 0.5,
            }}
          >
            {item.label}
          </Button>
        ))}
      </Paper>
    </Box>
  );
}
