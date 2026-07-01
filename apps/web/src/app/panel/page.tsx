import { redirect } from "next/navigation";

/**
 * Punto de entrada estable al panel durante la construcción incremental.
 *
 * La verificación es la primera función privada real disponible. Las fases de
 * perfil y reservas sustituirán este redirect por el resumen operativo.
 */
export default function VenuePanelPage() {
  redirect("/panel/verificacion");
}
