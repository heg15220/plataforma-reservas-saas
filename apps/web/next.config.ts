import type { NextConfig } from "next";

/**
 * Configuración base del frontend. Las integraciones de i18n, observabilidad
 * y despliegue se incorporarán en sus tareas específicas.
 */
const nextConfig: NextConfig = {
  reactStrictMode: true,
};

export default nextConfig;
