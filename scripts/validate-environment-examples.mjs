import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const templates = [".env.local.example", ".env.staging.example", ".env.production.example"];

const requiredKeys = [
  "RESERLY_ENVIRONMENT",
  "SPRING_PROFILES_ACTIVE",
  "RESERLY_PUBLIC_BASE_URL",
  "RESERLY_WEB_BASE_URL",
  "RESERLY_ALLOWED_ORIGINS",
  "NEXT_PUBLIC_APP_ENV",
  "NEXT_PUBLIC_API_BASE_URL",
  "RESERLY_API_INTERNAL_URL",
  "RESERLY_SECURE_COOKIES",
  "RESERLY_REAL_PAYMENTS_ENABLED",
  "RESERLY_DATABASE_NAME",
  "RESERLY_DATABASE_PORT",
  "RESERLY_DATABASE_URL",
  "RESERLY_DATABASE_USERNAME",
  "RESERLY_DATABASE_PASSWORD",
  "RESERLY_REDIS_PORT",
  "RESERLY_REDIS_PASSWORD",
  "RESERLY_REDIS_URL",
  "RESERLY_RABBITMQ_PORT",
  "RESERLY_RABBITMQ_MANAGEMENT_PORT",
  "RESERLY_RABBITMQ_USERNAME",
  "RESERLY_RABBITMQ_PASSWORD",
  "RESERLY_RABBITMQ_URL",
  "RESERLY_S3_ENDPOINT",
  "RESERLY_S3_BUCKET",
  "RESERLY_S3_ACCESS_KEY",
  "RESERLY_S3_SECRET_KEY",
];

const forbiddenPublicFragments = [
  "SECRET",
  "PASSWORD",
  "TOKEN",
  "PRIVATE",
  "ACCESS_KEY",
  "CREDENTIAL",
];

/**
 * Convierte una plantilla dotenv sencilla en un mapa sin expandir valores.
 *
 * @param {string} contents contenido UTF-8 de la plantilla
 * @returns {Map<string, string>} variables declaradas
 */
function parseTemplate(contents) {
  const values = new Map();

  for (const line of contents.split(/\r?\n/u)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const separator = trimmed.indexOf("=");
    if (separator < 1) {
      throw new Error(`Línea dotenv inválida: ${line}`);
    }

    values.set(trimmed.slice(0, separator), trimmed.slice(separator + 1));
  }

  return values;
}

for (const template of templates) {
  const contents = await readFile(resolve(template), "utf8");
  const values = parseTemplate(contents);
  const missingKeys = requiredKeys.filter((key) => !values.has(key));

  if (missingKeys.length > 0) {
    throw new Error(`${template} no declara: ${missingKeys.join(", ")}`);
  }

  for (const key of values.keys()) {
    if (
      key.startsWith("NEXT_PUBLIC_") &&
      forbiddenPublicFragments.some((fragment) => key.includes(fragment))
    ) {
      throw new Error(`${template} expone una variable potencialmente secreta: ${key}`);
    }
  }

  const environment = values.get("RESERLY_ENVIRONMENT");
  if (environment !== values.get("NEXT_PUBLIC_APP_ENV")) {
    throw new Error(`${template} declara entornos distintos para API y web`);
  }

  if (environment !== "local") {
    for (const key of [
      "RESERLY_PUBLIC_BASE_URL",
      "RESERLY_WEB_BASE_URL",
      "NEXT_PUBLIC_API_BASE_URL",
    ]) {
      if (!values.get(key)?.startsWith("https://")) {
        throw new Error(`${template} debe usar HTTPS en ${key}`);
      }
    }

    if (values.get("RESERLY_SECURE_COOKIES") !== "true") {
      throw new Error(`${template} debe activar cookies seguras`);
    }
  }

  if (values.get("RESERLY_REAL_PAYMENTS_ENABLED") !== "false") {
    throw new Error(`${template} no puede activar pagos reales`);
  }
}

console.log(`Plantillas de entorno válidas: ${templates.join(", ")}`);
