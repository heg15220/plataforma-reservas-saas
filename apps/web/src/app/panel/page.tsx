import { redirect } from "next/navigation";

/**
 * Punto de entrada estable al panel operativo.
 *
 * La agenda diaria es la primera vista accionable tras el acceso del local.
 */
export default function VenuePanelPage() {
  redirect("/panel/reservas");
}
