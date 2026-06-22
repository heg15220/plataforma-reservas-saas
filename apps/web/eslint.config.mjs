import { defineConfig, globalIgnores } from "eslint/config";
import prettier from "eslint-config-prettier/flat";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTypeScript from "eslint-config-next/typescript";

/**
 * Reglas estáticas del frontend. Core Web Vitals y TypeScript se tratan como
 * errores para que el mismo comando sea válido en local y en CI.
 */
export default defineConfig([
  ...nextVitals,
  ...nextTypeScript,
  prettier,
  globalIgnores([".next/**", "coverage/**", "out/**", "next-env.d.ts"]),
]);
