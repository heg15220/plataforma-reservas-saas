import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { TextDecoder } from "node:util";

const repoRoot = fileURLToPath(new URL("../", import.meta.url));
const utf8Decoder = new TextDecoder("utf-8", { fatal: true });

const ignoredDirectoryNames = new Set([
  ".git",
  ".next",
  "coverage",
  "dist",
  "node_modules",
  "out",
  "target",
]);

const ignoredFileNames = new Set(["package-lock.json"]);
const textExtensions = new Set([
  ".css",
  ".env",
  ".example",
  ".java",
  ".json",
  ".md",
  ".mjs",
  ".properties",
  ".sql",
  ".ts",
  ".tsx",
  ".txt",
  ".yaml",
  ".yml",
]);

const spanishQualityPathPatterns = [
  /^\.kiro\/specs\/.*\.md$/u,
  /^apps\/api\/src\/main\/resources\/.*\.(properties|sql|ya?ml)$/u,
  /^apps\/web\/locales\/es\.json$/u,
  /^docs\/.*\.md$/u,
  /(^|\/)README\.md$/u,
  /^CONTRIBUTING\.md$/u,
  /^\.env\..*\.example$/u,
  /(^|\/)(emails?|mail|templates?|seeds?|fixtures?)\//u,
];

const mojibakePatterns = [/\u00c3/u, /\u00c2/u, /\u00e2/u, /\u00ef\u00bf\u00bd/u, /\ufffd/u];

const spanishMarkers = [
  "acción",
  "acciones",
  "administración",
  "aplicación",
  "asíncrono",
  "búsqueda",
  "catálogo",
  "codificación",
  "configuración",
  "contraseña",
  "documentación",
  "español",
  "inglés",
  "móvil",
  "página",
  "público",
  "resolución",
  "sección",
  "técnico",
  "validación",
  "verificación",
];

const missingAccentTerms = new Map([
  ["accion", "acción"],
  ["administracion", "administración"],
  ["aplicacion", "aplicación"],
  ["asincrono", "asíncrono"],
  ["asincronos", "asíncronos"],
  ["busqueda", "búsqueda"],
  ["catalogo", "catálogo"],
  ["catalogos", "catálogos"],
  ["codigo", "código"],
  ["codificacion", "codificación"],
  ["configuracion", "configuración"],
  ["contrasena", "contraseña"],
  ["documentacion", "documentación"],
  ["ejecucion", "ejecución"],
  ["espanol", "español"],
  ["ingles", "inglés"],
  ["movil", "móvil"],
  ["pagina", "página"],
  ["proxima", "próxima"],
  ["proximo", "próximo"],
  ["publicacion", "publicación"],
  ["publico", "público"],
  ["raiz", "raíz"],
  ["resolucion", "resolución"],
  ["seccion", "sección"],
  ["tecnico", "técnico"],
  ["validacion", "validación"],
  ["verificacion", "verificación"],
]);

const files = await listTextFiles(repoRoot);
const errors = [];

for (const file of files) {
  await validateFile(file, errors);
}

if (errors.length > 0) {
  console.error("Validación de codificación y calidad de español inválida:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exitCode = 1;
} else {
  console.log(
    "Validación de español correcta: UTF-8, mojibake, tildes frecuentes y signos de apertura.",
  );
}

async function validateFile(file, errors) {
  const buffer = await readFile(file);
  const relativePath = toRelativePath(file);
  let source;

  try {
    source = utf8Decoder.decode(buffer);
  } catch {
    errors.push(`${relativePath}: el archivo no se puede decodificar como UTF-8 válido.`);
    return;
  }

  validateNoMojibake(relativePath, source, errors);

  if (isSpanishQualityFile(relativePath)) {
    validateSpanishQuality(relativePath, source, errors);
  }
}

function validateNoMojibake(relativePath, source, errors) {
  const lines = source.split(/\r?\n/u);

  for (const [lineIndex, line] of lines.entries()) {
    const textOutsideInlineCode = line.replace(/`[^`]*`/gu, "");

    if (mojibakePatterns.some((pattern) => pattern.test(textOutsideInlineCode))) {
      errors.push(
        `${relativePath}:${lineIndex + 1} posible mojibake o carácter de sustitución: ${line.trim()}`,
      );
    }
  }
}

function validateSpanishQuality(relativePath, source, errors) {
  const lines = source.split(/\r?\n/u);
  let insideCodeFence = false;

  for (const [lineIndex, line] of lines.entries()) {
    const trimmedLine = line.trim();

    if (trimmedLine.startsWith("```")) {
      insideCodeFence = !insideCodeFence;
      continue;
    }

    if (insideCodeFence || shouldSkipQualityLine(trimmedLine)) {
      continue;
    }

    const visibleText = normalizeVisibleText(trimmedLine);

    if (!looksLikeSpanish(visibleText)) {
      continue;
    }

    validateOpeningSigns(relativePath, lineIndex + 1, visibleText, errors);
    validateCommonAccents(relativePath, lineIndex + 1, visibleText, errors);
  }
}

function validateOpeningSigns(relativePath, lineNumber, text, errors) {
  if (text.includes("?") && !text.includes("¿")) {
    errors.push(`${relativePath}:${lineNumber} una pregunta en español debe abrir con ¿.`);
  }

  if (text.includes("!") && !text.includes("¡")) {
    errors.push(`${relativePath}:${lineNumber} una exclamación en español debe abrir con ¡.`);
  }
}

function validateCommonAccents(relativePath, lineNumber, text, errors) {
  const lowerText = text.toLocaleLowerCase("es");

  for (const [plain, accented] of missingAccentTerms.entries()) {
    if (new RegExp(`\\b${plain}\\b`, "u").test(lowerText)) {
      errors.push(
        `${relativePath}:${lineNumber} posible palabra española sin tilde: "${plain}" debería ser "${accented}".`,
      );
    }
  }
}

function normalizeVisibleText(line) {
  return line
    .replace(/`[^`]*`/gu, " ")
    .replace(/\[[^\]]+\]\([^)]+\)/gu, " ")
    .replace(/https?:\/\/\S+/gu, " ")
    .replace(/[{}[\]()"',:;=<>/\\|*_#-]/gu, " ")
    .replace(/\s+/gu, " ")
    .trim();
}

function looksLikeSpanish(text) {
  const lowerText = text.toLocaleLowerCase("es");

  return (
    /[áéíóúüñ¿¡]/u.test(text) ||
    spanishMarkers.some((marker) => lowerText.includes(marker)) ||
    /\b(el|la|los|las|de|del|para|con|sin|cuando|texto|textos|archivo|archivos)\b/u.test(lowerText)
  );
}

function shouldSkipQualityLine(line) {
  return (
    line.length === 0 ||
    line.startsWith("|") ||
    line.startsWith("import ") ||
    line.startsWith("export ") ||
    line.startsWith("const ") ||
    line.startsWith("let ") ||
    line.startsWith("function ") ||
    line.startsWith("//") ||
    /^[A-Z0-9_]+=/u.test(line)
  );
}

function isSpanishQualityFile(relativePath) {
  return spanishQualityPathPatterns.some((pattern) => pattern.test(relativePath));
}

async function listTextFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    if (ignoredDirectoryNames.has(entry.name)) {
      continue;
    }

    const entryPath = path.join(directory, entry.name);

    if (entry.isDirectory()) {
      files.push(...(await listTextFiles(entryPath)));
      continue;
    }

    if (entry.isFile() && isTextFile(entryPath)) {
      files.push(entryPath);
    }
  }

  return files;
}

function isTextFile(file) {
  const relativePath = toRelativePath(file);
  const extension = path.extname(file);

  return (
    !ignoredFileNames.has(path.basename(file)) && textExtensions.has(extension || relativePath)
  );
}

function toRelativePath(file) {
  return path.relative(repoRoot, file).replaceAll(path.sep, "/");
}
