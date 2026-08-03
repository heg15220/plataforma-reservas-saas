import { notFound } from "next/navigation";
import { getLocale } from "next-intl/server";

import { fetchPublicAvailability } from "@/features/availability/availability-api";
import {
  PublicReservationFormView,
  type ReservationSummary,
} from "@/features/public-reservation/public-reservation-form";
import { getPublicVenue, resolvePublicAssetUrl } from "@/features/public-venue/public-venue-api";

type ReservationPageProps = {
  params: Promise<{ slug: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

/**
 * Resolves every visible booking detail from current public API projections.
 * Query parameters identify the selection but never become trusted display data.
 */
export default async function ReservationPage({ params, searchParams }: ReservationPageProps) {
  const { slug } = await params;
  const query = await searchParams;
  const locale = await getLocale();
  const timeSlotId = first(query.slotId);
  const date = first(query.date);
  if (!timeSlotId || !date) {
    notFound();
  }

  const [venue, availability] = await Promise.all([
    getPublicVenue(slug, locale),
    fetchPublicAvailability(slug, date, locale),
  ]);
  const slot = availability.slots.find((candidate) => candidate.slotId === timeSlotId);
  if (!slot) {
    notFound();
  }

  const employeeResourceId = first(query.employeeResourceId);
  const resource = slot.availableEmployeeResources.find(
    (candidate) => candidate.employeeResourceId === employeeResourceId,
  );
  const summary: ReservationSummary = {
    venueName: venue.name,
    venueCategory: venue.categoryName,
    venueAddress: [venue.address, venue.city, venue.province].filter(Boolean).join(", "),
    venueImageUrl: venue.mainImageUrl ? resolvePublicAssetUrl(venue.mainImageUrl) : null,
    date: availability.date,
    startsAt: slot.startsAt,
    endsAt: slot.endsAt,
    bookingMode: slot.bookingMode,
    serviceName: slot.serviceName,
    resourceName: resource?.displayName ?? null,
    bookingRules: venue.rules,
  };

  return (
    <PublicReservationFormView
      assignmentPreference={first(query.assignmentPreference)}
      employeeResourceId={employeeResourceId}
      reservationSummary={summary}
      serviceId={slot.serviceId ?? undefined}
      timeSlotId={timeSlotId}
      venueSlug={slug}
    />
  );
}

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}
