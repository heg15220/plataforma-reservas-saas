import { notFound } from "next/navigation";
import { PublicReservationFormView } from "@/features/public-reservation/public-reservation-form";

export default async function ReservationPage({ params, searchParams }: { params: Promise<{ slug: string }>; searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const { slug } = await params; const query = await searchParams;
  const first = (value: string | string[] | undefined) => Array.isArray(value) ? value[0] : value;
  const timeSlotId = first(query.slotId);
  if (!timeSlotId) notFound();
  return <PublicReservationFormView venueSlug={slug} timeSlotId={timeSlotId} serviceId={first(query.serviceId)} employeeResourceId={first(query.employeeResourceId)} assignmentPreference={first(query.assignmentPreference)} />;
}