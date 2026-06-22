import type { Metadata } from "next";
import type { ReactNode } from "react";

import "./globals.css";

export const metadata: Metadata = {
  title: "Reserly",
  description: "Online booking platform",
};

/**
 * Layout raíz compartido por la web pública y los futuros paneles.
 *
 * @param children contenido de la ruta activa
 */
export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
