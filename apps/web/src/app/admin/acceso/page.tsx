import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageContainer, Surface } from "@/components/layout";
import { AdminLoginForm } from "@/features/admin/admin-login-form";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Admin.login");
  return { title: t("metadata"), robots: { index: false, follow: false } };
}

/** Acceso segregado para cuentas administrativas. */
export default function AdminLoginPage() {
  return (
    <PageContainer compact>
      <Surface component="main" sx={{ maxWidth: 520, mx: "auto", mt: { xs: 6, md: 12 } }}>
        <AdminLoginForm />
      </Surface>
    </PageContainer>
  );
}
