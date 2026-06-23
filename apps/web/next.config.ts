import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

import { loadWebEnvironment } from "./environment";

const environment = loadWebEnvironment();

/**
 * Configuración base del frontend.
 *
 * La lectura tipada garantiza que un despliegue no se compile con URLs
 * incompletas, perfiles desconocidos o HTTP público fuera de local/test.
 */
const nextConfig: NextConfig = {
  reactStrictMode: true,
  env: {
    NEXT_PUBLIC_APP_ENV: environment.appEnvironment,
    NEXT_PUBLIC_API_BASE_URL: environment.publicApiBaseUrl,
  },
};

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

export default withNextIntl(nextConfig);
