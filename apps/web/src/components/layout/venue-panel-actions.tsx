"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Snackbar from "@mui/material/Snackbar";
import Stack from "@mui/material/Stack";
import Tooltip from "@mui/material/Tooltip";
import { House, LogOut } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { NavigationLink } from "@/components/navigation-link";
import { logoutVenue } from "@/features/venue-login/venue-login-api";

export interface VenuePanelActionsProps {
  compact?: boolean;
}

/**
 * Acciones globales del propietario disponibles desde cualquier pantalla privada.
 *
 * El logout espera la revocación de la cookie HttpOnly en backend antes de abandonar
 * el panel. Si la red falla, conserva la sesión y permite reintentar sin simular éxito.
 */
export function VenuePanelActions({ compact = false }: VenuePanelActionsProps) {
  const t = useTranslations("Navigation.venue");
  const router = useRouter();
  const abortControllerRef = useRef<AbortController | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutFailed, setLogoutFailed] = useState(false);

  useEffect(
    () => () => {
      abortControllerRef.current?.abort();
    },
    [],
  );

  async function handleLogout() {
    if (loggingOut) return;

    const controller = new AbortController();
    abortControllerRef.current = controller;
    setLoggingOut(true);
    setLogoutFailed(false);

    try {
      await logoutVenue(controller.signal);
      router.replace("/");
      router.refresh();
    } catch {
      if (!controller.signal.aborted) {
        setLogoutFailed(true);
        setLoggingOut(false);
      }
    } finally {
      abortControllerRef.current = null;
    }
  }

  return (
    <>
      {compact ? (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title={t("publicHome")}>
            <IconButton
              aria-label={t("publicHome")}
              color="inherit"
              component={NavigationLink}
              href="/"
            >
              <House aria-hidden="true" size={20} />
            </IconButton>
          </Tooltip>
          <Tooltip title={loggingOut ? t("loggingOut") : t("logout")}>
            <span>
              <IconButton
                aria-label={loggingOut ? t("loggingOut") : t("logout")}
                color="inherit"
                disabled={loggingOut}
                onClick={() => void handleLogout()}
              >
                {loggingOut ? (
                  <CircularProgress aria-hidden="true" color="inherit" size={18} />
                ) : (
                  <LogOut aria-hidden="true" size={20} />
                )}
              </IconButton>
            </span>
          </Tooltip>
        </Stack>
      ) : (
        <Stack spacing={0.5}>
          <Button
            component={NavigationLink}
            href="/"
            startIcon={<House aria-hidden="true" size={18} />}
            sx={{ color: "common.white", justifyContent: "flex-start", px: 2.5 }}
          >
            {t("publicHome")}
          </Button>
          <Button
            disabled={loggingOut}
            onClick={() => void handleLogout()}
            startIcon={
              loggingOut ? (
                <CircularProgress aria-hidden="true" color="inherit" size={16} />
              ) : (
                <LogOut aria-hidden="true" size={18} />
              )
            }
            sx={{ color: "common.white", justifyContent: "flex-start", px: 2.5 }}
          >
            {loggingOut ? t("loggingOut") : t("logout")}
          </Button>
        </Stack>
      )}

      <Snackbar
        anchorOrigin={{ horizontal: "center", vertical: "bottom" }}
        autoHideDuration={6000}
        onClose={() => setLogoutFailed(false)}
        open={logoutFailed}
      >
        <Alert onClose={() => setLogoutFailed(false)} severity="error" variant="filled">
          {t("logoutError")}
        </Alert>
      </Snackbar>
    </>
  );
}
