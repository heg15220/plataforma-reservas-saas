import { PublicReservationConfirmation } from "@/features/reservation-booking/public-reservation-confirmation";

interface ReservationConfirmationPageProps {
  params: Promise<{ id: string }>;
}

/** Ruta final; el UUID solo identifica el snapshot aislado de la sesión del navegador. */
export default async function ReservationConfirmationPage({ params }: ReservationConfirmationPageProps) {
  const { id } = await params;
  return <PublicReservationConfirmation reservationId={id} />;
}