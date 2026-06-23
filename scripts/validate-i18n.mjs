import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const repoRoot = new URL("../", import.meta.url);
const webRoot = new URL("../apps/web/", import.meta.url);
const localeDirectory = new URL("./locales/", webRoot);
const sourceDirectory = new URL("./src/", webRoot);

const visibleAttributeNames = new Set([
  "alt",
  "aria-label",
  "helperText",
  "label",
  "placeholder",
  "primary",
  "secondary",
  "title",
  "tooltip",
]);

const ignoredDirectoryNames = new Set([".next", "node_modules"]);
const uiFilePattern = /\.(tsx)$/;
const testFilePattern = /\.(test|spec)\.tsx$/;

const catalogValidation = await validateCatalogs();
const errors = [
  ...catalogValidation.errors,
  ...(await validateUiContracts(catalogValidation.referenceKeys)),
];

if (errors.length > 0) {
  console.error("Validación i18n inválida:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exitCode = 1;
} else {
  console.log("Validación i18n correcta: catálogos completos y UI sin texto visible hardcodeado.");
}

async function validateCatalogs() {
  const [esMessages, enMessages] = await Promise.all([
    readJson(new URL("es.json", localeDirectory)),
    readJson(new URL("en.json", localeDirectory)),
  ]);

  const esKeys = flattenMessageKeys(esMessages).sort();
  const enKeys = flattenMessageKeys(enMessages).sort();
  const esKeySet = new Set(esKeys);
  const enKeySet = new Set(enKeys);
  const missingInEnglish = esKeys.filter((key) => !enKeySet.has(key));
  const missingInSpanish = enKeys.filter((key) => !esKeySet.has(key));
  const errors = [];

  for (const key of missingInEnglish) {
    errors.push(`Falta la clave inglesa locales/en.json: ${key}`);
  }

  for (const key of missingInSpanish) {
    errors.push(`Falta la clave española locales/es.json: ${key}`);
  }

  return {
    errors,
    referenceKeys: esKeySet,
  };
}

async function validateUiContracts(referenceKeys) {
  const files = (await listFiles(sourceDirectory)).filter(
    (file) => uiFilePattern.test(file) && !testFilePattern.test(file),
  );
  const errors = [];

  for (const file of files) {
    const source = await readFile(file, "utf8");
    const sourceFile = ts.createSourceFile(
      file,
      source,
      ts.ScriptTarget.Latest,
      true,
      ts.ScriptKind.TSX,
    );
    inspectNode(sourceFile, sourceFile, errors, new Map(), referenceKeys);
  }

  return errors;
}

function inspectNode(node, sourceFile, errors, translationAliases, referenceKeys) {
  const scopedTranslationAliases = isFunctionScopeNode(node)
    ? new Map(translationAliases)
    : translationAliases;

  if (ts.isVariableDeclaration(node) && ts.isIdentifier(node.name)) {
    const namespace = readTranslationNamespace(node.initializer);

    if (namespace) {
      scopedTranslationAliases.set(node.name.text, namespace);
    }
  }

  if (ts.isJsxText(node)) {
    const text = normalizeJsxText(node.getText(sourceFile));

    if (isVisibleText(text)) {
      errors.push(formatNodeError(sourceFile, node, `Texto JSX hardcodeado: "${text}"`));
    }
  }

  if (ts.isJsxExpression(node) && node.expression && isStringLikeNode(node.expression)) {
    const text = node.expression.text;

    if (isVisibleText(text)) {
      errors.push(formatNodeError(sourceFile, node.expression, `Texto JSX hardcodeado: "${text}"`));
    }
  }

  if (ts.isJsxExpression(node) && node.expression && ts.isTemplateExpression(node.expression)) {
    const staticText = readTemplateStaticText(node.expression);

    if (isVisibleTemplateText(staticText)) {
      errors.push(
        formatNodeError(sourceFile, node.expression, `Texto JSX hardcodeado: "${staticText}"`),
      );
    }
  }

  if (ts.isJsxAttribute(node) && visibleAttributeNames.has(node.name.getText(sourceFile))) {
    const text = getJsxAttributeStringValue(node);

    if (text && isVisibleText(text)) {
      errors.push(
        formatNodeError(
          sourceFile,
          node,
          `Atributo visible hardcodeado "${node.name.getText(sourceFile)}": "${text}"`,
        ),
      );
    }

    const staticText = getJsxAttributeTemplateStaticText(node);

    if (isVisibleTemplateText(staticText)) {
      errors.push(
        formatNodeError(
          sourceFile,
          node,
          `Atributo visible hardcodeado "${node.name.getText(sourceFile)}": "${staticText}"`,
        ),
      );
    }
  }

  validateTranslationKeyReference(
    node,
    sourceFile,
    errors,
    scopedTranslationAliases,
    referenceKeys,
  );

  ts.forEachChild(node, (child) =>
    inspectNode(child, sourceFile, errors, scopedTranslationAliases, referenceKeys),
  );
}

function validateTranslationKeyReference(
  node,
  sourceFile,
  errors,
  translationAliases,
  referenceKeys,
) {
  if (!ts.isCallExpression(node) || !ts.isIdentifier(node.expression)) {
    validateDirectTranslationCall(node, sourceFile, errors, referenceKeys);
    return;
  }

  const namespace = translationAliases.get(node.expression.text);
  const [keyArgument] = node.arguments;

  if (!namespace || !keyArgument || !isStringLikeNode(keyArgument)) {
    return;
  }

  const fullKey = `${namespace}.${keyArgument.text}`;

  if (!referenceKeys.has(fullKey)) {
    errors.push(formatNodeError(sourceFile, keyArgument, `Clave i18n no encontrada: ${fullKey}`));
  }
}

function validateDirectTranslationCall(node, sourceFile, errors, referenceKeys) {
  if (
    !ts.isCallExpression(node) ||
    !ts.isCallExpression(node.expression) ||
    !isTranslationFactoryCall(node.expression)
  ) {
    return;
  }

  const namespaceArgument = node.expression.arguments[0];
  const keyArgument = node.arguments[0];

  if (!isStringLikeNode(namespaceArgument) || !keyArgument || !isStringLikeNode(keyArgument)) {
    return;
  }

  const fullKey = `${namespaceArgument.text}.${keyArgument.text}`;

  if (!referenceKeys.has(fullKey)) {
    errors.push(formatNodeError(sourceFile, keyArgument, `Clave i18n no encontrada: ${fullKey}`));
  }
}

function getJsxAttributeStringValue(attribute) {
  if (!attribute.initializer) {
    return undefined;
  }

  if (ts.isStringLiteral(attribute.initializer)) {
    return attribute.initializer.text;
  }

  if (
    ts.isJsxExpression(attribute.initializer) &&
    attribute.initializer.expression &&
    isStringLikeNode(attribute.initializer.expression)
  ) {
    return attribute.initializer.expression.text;
  }

  return undefined;
}

function getJsxAttributeTemplateStaticText(attribute) {
  if (
    !attribute.initializer ||
    !ts.isJsxExpression(attribute.initializer) ||
    !attribute.initializer.expression ||
    !ts.isTemplateExpression(attribute.initializer.expression)
  ) {
    return undefined;
  }

  return readTemplateStaticText(attribute.initializer.expression);
}

function isStringLikeNode(node) {
  return ts.isStringLiteral(node) || ts.isNoSubstitutionTemplateLiteral(node);
}

function isFunctionScopeNode(node) {
  return (
    ts.isFunctionDeclaration(node) ||
    ts.isFunctionExpression(node) ||
    ts.isArrowFunction(node) ||
    ts.isMethodDeclaration(node)
  );
}

function readTranslationNamespace(node) {
  if (!node) {
    return undefined;
  }

  const expression = ts.isAwaitExpression(node) ? node.expression : node;

  if (ts.isCallExpression(expression) && isTranslationFactoryCall(expression)) {
    return expression.arguments[0].text;
  }

  return undefined;
}

function isTranslationFactoryCall(node) {
  return (
    ts.isIdentifier(node.expression) &&
    ["getTranslations", "useTranslations"].includes(node.expression.text) &&
    node.arguments.length === 1 &&
    isStringLikeNode(node.arguments[0])
  );
}

function readTemplateStaticText(templateExpression) {
  const parts = [templateExpression.head.text];

  for (const span of templateExpression.templateSpans) {
    parts.push(span.literal.text);
  }

  return parts.join("").replace(/\s+/g, " ").trim();
}

function normalizeJsxText(value) {
  return value.replace(/\s+/g, " ").trim();
}

function isVisibleText(value) {
  return value.trim().length > 0;
}

function isVisibleTemplateText(value) {
  return Boolean(value && /[\p{L}\p{N}]/u.test(value));
}

function flattenMessageKeys(value, prefix = "") {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return [prefix];
  }

  return Object.entries(value).flatMap(([key, nestedValue]) =>
    flattenMessageKeys(nestedValue, prefix ? `${prefix}.${key}` : key),
  );
}

async function listFiles(directoryUrl) {
  return listFilesInDirectory(fileURLToPath(directoryUrl));
}

async function listFilesInDirectory(directoryPath) {
  const entries = await readdir(directoryPath, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const entryPath = path.join(directoryPath, entry.name);

    if (entry.isDirectory()) {
      if (!ignoredDirectoryNames.has(entry.name)) {
        files.push(...(await listFilesInDirectory(entryPath)));
      }
      continue;
    }

    if (entry.isFile()) {
      files.push(entryPath);
    }
  }

  return files;
}

function formatNodeError(sourceFile, node, message) {
  const position = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile));
  const relativePath = path
    .relative(fileURLToPath(repoRoot), sourceFile.fileName)
    .replaceAll(path.sep, "/");

  return `${relativePath}:${position.line + 1}:${position.character + 1} ${message}`;
}

async function readJson(fileUrl) {
  return JSON.parse(await readFile(fileUrl, "utf8"));
}
