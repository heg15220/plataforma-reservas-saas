import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

/**
 * Entorno unitario para componentes síncronos de React.
 *
 * Los Server Components asíncronos se validarán con pruebas end-to-end porque
 * Vitest no reproduce actualmente su ciclo de ejecución completo.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    tsconfigPaths: true,
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
  },
});
