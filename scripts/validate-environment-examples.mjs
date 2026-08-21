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
  "RESERLY_REDSYS_PAYMENT_ENDPOINT",
  "RESERLY_REDSYS_MERCHANT_CODE",
  "RESERLY_REDSYS_TERMINAL",
  "RESERLY_REDSYS_SIGNING_KEY",
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
  "RESERLY_MLFLOW_TRACKING_URI",
  "RESERLY_MLFLOW_DATABASE_NAME",
  "RESERLY_MLFLOW_DATABASE_USERNAME",
  "RESERLY_MLFLOW_DATABASE_PASSWORD",
  "RESERLY_MLFLOW_ADMIN_USERNAME",
  "RESERLY_MLFLOW_ADMIN_PASSWORD",
  "RESERLY_MLFLOW_TRAINING_USERNAME",
  "RESERLY_MLFLOW_TRAINING_PASSWORD",
  "RESERLY_MLFLOW_TRAINING_SECRET_VERSION",
  "RESERLY_MLFLOW_REGISTRATION_USERNAME",
  "RESERLY_MLFLOW_REGISTRATION_PASSWORD",
  "RESERLY_MLFLOW_REGISTRATION_SECRET_VERSION",
  "RESERLY_MLFLOW_INFERENCE_USERNAME",
  "RESERLY_MLFLOW_INFERENCE_PASSWORD",
  "RESERLY_MLFLOW_INFERENCE_SECRET_VERSION",
  "RESERLY_MLFLOW_FLASK_SECRET_KEY",
  "RESERLY_MLFLOW_ALLOWED_HOSTS",
  "RESERLY_MLFLOW_S3_BUCKET",
  "RESERLY_PREFECT_API_URL",
  "RESERLY_PREFECT_DATABASE_NAME",
  "RESERLY_PREFECT_DATABASE_USERNAME",
  "RESERLY_PREFECT_DATABASE_PASSWORD",
  "RESERLY_PREFECT_AUTH_USERNAME",
  "RESERLY_PREFECT_AUTH_PASSWORD",
  "RESERLY_PREFECT_CORS_ALLOWED_ORIGINS",
  "RESERLY_PROMETHEUS_PORT",
  "RESERLY_GRAFANA_PORT",
  "RESERLY_GRAFANA_ADMIN_USERNAME",
  "RESERLY_GRAFANA_ADMIN_PASSWORD",
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

  for (const purpose of ["TRAINING", "REGISTRATION", "INFERENCE"]) {
    const expected = `reserly-${environment}-${purpose.toLowerCase()}-v1`;
    if (values.get(`RESERLY_MLFLOW_${purpose}_USERNAME`) !== expected) {
      throw new Error(`${template} no separa/versiona el principal MLflow ${purpose}`);
    }
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

  const expectedRedsysEndpoint =
    environment === "production"
      ? "https://sis.redsys.es/sis/realizarPago"
      : "https://sis-t.redsys.es:25443/sis/realizarPago";
  if (values.get("RESERLY_REDSYS_PAYMENT_ENDPOINT") !== expectedRedsysEndpoint) {
    throw new Error(`${template} no usa el endpoint RedSys esperado`);
  }
  for (const key of [
    "RESERLY_REDSYS_MERCHANT_CODE",
    "RESERLY_REDSYS_TERMINAL",
    "RESERLY_REDSYS_SIGNING_KEY",
  ]) {
    if (values.get(key) !== "") {
      throw new Error(`${template} no debe contener credenciales RedSys`);
    }
  }
}

const webPackage = JSON.parse(await readFile(resolve("apps/web/package.json"), "utf8"));
if (webPackage.scripts?.dev !== "dotenv -e ../../.env.local -- next dev") {
  throw new Error(
    "apps/web debe cargar el .env.local de la raíz antes de iniciar Next.js en desarrollo",
  );
}

console.log(`Plantillas de entorno válidas: ${templates.join(", ")}`);
