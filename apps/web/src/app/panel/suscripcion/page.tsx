import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueSubscriptionDashboard } from "@/features/venue-subscription/venue-subscription-dashboard";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueSubscription.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Pantalla privada responsive de suscripción y estado de monetización. */
export default async function VenueSubscriptionPage() {
  const t = await getTranslations("VenueSubscription.hero");
  return (
    <VenueShell currentPath="/panel/suscripcion">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueSubscriptionDashboard />
    </VenueShell>
  );
}
