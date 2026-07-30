"use client";

import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";

import { NavigationLink } from "@/components/navigation-link";

import { Brand } from "./brand";
import { PageContainer } from "./page-container";

/** Shell administrativo responsive con navegación limitada al alcance implementado. */
export function AdminShell({
  children,
  currentPath,
}: {
  children: ReactNode;
  currentPath: string;
}) {
  const t = useTranslations("Admin.navigation");
  return (
    <Box sx={{ minHeight: "100dvh" }}>
      <Box component="header" sx={{ borderBottom: 1, borderColor: "divider", py: 2 }}>
        <PageContainer compact>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            sx={{ alignItems: { sm: "center" } }}
          >
            <Brand />
            <Stack
              component="nav"
              direction="row"
              spacing={1}
              useFlexGap
              sx={{ flexWrap: "wrap", ml: { sm: "auto" } }}
            >
              {[
                ["/admin/categorias", "categories"],
                ["/admin/locales", "venues"],
                ["/admin/incidencias", "incidents"],
                ["/admin/verificaciones", "businessAccounts"],
                ["/admin/penalizaciones", "penalties"],
              ].map(([href, key]) => (
                <Button
                  aria-current={currentPath === href ? "page" : undefined}
                  component={NavigationLink}
                  href={href}
                  key={href}
                  variant={currentPath === href ? "contained" : "text"}
                >
                  {t(key)}
                </Button>
              ))}
            </Stack>
          </Stack>
        </PageContainer>
      </Box>
      <Box component="main" sx={{ py: { xs: 5, md: 8 } }}>
        <PageContainer compact>{children}</PageContainer>
      </Box>
    </Box>
  );
}
