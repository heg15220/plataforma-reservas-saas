import { PublicReservationManagement } from "@/features/reservation-management/public-reservation-management";

interface ReservationManagementPageProps {
  params: Promise<{ token: string }>;
}

/** Entrada pública; el secreto se entrega directamente al cliente y nunca se persiste en storage. */
export default async function ReservationManagementPage({ params }: ReservationManagementPageProps) {
  const { token } = await params;
  return <PublicReservationManagement token={token} />;
}
