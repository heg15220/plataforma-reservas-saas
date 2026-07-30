import { redirect } from "next/navigation";

/** Entrada estable del panel administrativo. */
export default function AdminPage() {
  redirect("/admin/categorias");
}
