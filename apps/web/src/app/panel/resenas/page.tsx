import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueReviewsDashboard } from "@/features/venue-reviews/venue-reviews-dashboard";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueReviews.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Sección privada responsive de reputación y reseñas verificadas. */
export default async function VenueReviewsPage() {
  const t = await getTranslations("VenueReviews.hero");
  return (
    <VenueShell currentPath="/panel/resenas">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueReviewsDashboard />
    </VenueShell>
  );
}
