import type { Metadata } from "next";
import type { ReactNode } from "react";

import { AppProviders } from "./providers";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Reserly",
    template: "%s | Reserly",
  },
  description: "Plataforma responsive para descubrir y gestionar reservas.",
};

/**
 * Layout raíz compartido por la web pública y los futuros paneles.
 *
 * @param children contenido de la ruta activa
 */
export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="es">
      <body>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
