import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = new URL("../", import.meta.url);
const apiRoot = new URL("../apps/api/", import.meta.url);
const javaMainDirectory = new URL("./src/main/java/", apiRoot);
const migrationDirectory = new URL("./src/main/resources/db/migration/", apiRoot);

const upperCamelCasePattern = /^[A-Z][A-Za-z0-9]*$/;
const lowerCamelCasePattern = /^[a-z][A-Za-z0-9]*$/;
const relationAnnotations = new Set(["ManyToMany", "ManyToOne", "OneToMany", "OneToOne"]);
const ignoredDirectoryNames = new Set(["target"]);

const javaFiles = (await listFiles(javaMainDirectory)).filter((file) => file.endsWith(".java"));
const migrationFiles = (await listFiles(migrationDirectory)).filter((file) =>
  file.endsWith(".sql"),
);
const javaTypes = await readJavaTypes(javaFiles);
const errors = [];

for (const file of javaFiles) {
  await validateJavaFile(file, javaTypes, errors);
}

for (const file of migrationFiles) {
  await validateMigrationFile(file, errors);
}

if (errors.length > 0) {
  console.error("Convenciones backend inválidas:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exitCode = 1;
} else {
  console.log("Convenciones backend válidas: Java, JPA, DAOs, capas REST y migraciones.");
}

async function validateJavaFile(file, allJavaTypes, errors) {
  const source = await readFile(file, "utf8");
  const relativePath = toRelativePath(file);
  const type = readPrimaryJavaType(source);

  if (!type) {
    if (!relativePath.endsWith("package-info.java")) {
      errors.push(`${relativePath}: no se encontró tipo Java principal.`);
    }
    return;
  }

  const expectedFileName = `${type.name}.java`;
  if (path.basename(file) !== expectedFileName) {
    errors.push(`${relativePath}: el archivo debe llamarse ${expectedFileName}.`);
  }

  if (!upperCamelCasePattern.test(type.name)) {
    errors.push(`${relativePath}: la clase Java ${type.name} debe usar UpperCamelCase.`);
  }

  validateLayerContracts(relativePath, source, type, allJavaTypes, errors);
  validateJpaEntity(relativePath, source, type, errors);
  validateDao(relativePath, source, type, errors);
  validateDtoAndConverterNames(relativePath, type, errors);
}

function validateLayerContracts(relativePath, source, type, allJavaTypes, errors) {
  if (hasAnnotation(source, "Service")) {
    if (type.kind !== "class" || !type.name.endsWith("ServiceImpl")) {
      errors.push(
        `${relativePath}: las implementaciones @Service deben ser clases terminadas en ServiceImpl.`,
      );
    }

    const interfaceName = type.name.replace(/Impl$/, "");
    if (!allJavaTypes.has(interfaceName)) {
      errors.push(`${relativePath}: falta la interfaz de servicio ${interfaceName}.`);
    }
  }

  if (hasAnnotation(source, "RestController")) {
    if (type.kind !== "class" || !type.name.endsWith("ControllerImpl")) {
      errors.push(
        `${relativePath}: los @RestController deben ser clases terminadas en ControllerImpl.`,
      );
    }

    const interfaceName = type.name.replace(/Impl$/, "");
    if (!allJavaTypes.has(interfaceName)) {
      errors.push(`${relativePath}: falta la interfaz de controlador ${interfaceName}.`);
    }
  }
}

function validateJpaEntity(relativePath, source, type, errors) {
  if (!hasAnnotation(source, "Entity")) {
    return;
  }

  const tableName = readAnnotationName(source, "Table");
  if (!tableName) {
    errors.push(
      `${relativePath}: las entidades deben declarar @Table(name = "\"UpperCamelCase\"").`,
    );
  } else {
    validateQuotedUpperCamelIdentifier(relativePath, "tabla JPA", tableName, errors);
  }

  for (const columnName of readAnnotationNames(source, "Column")) {
    validateQuotedLowerCamelIdentifier(relativePath, "columna JPA", columnName, errors);
  }

  for (const columnName of readAnnotationNames(source, "JoinColumn")) {
    validateQuotedLowerCamelIdentifier(relativePath, "columna de relación JPA", columnName, errors);
  }

  validateJpaRelationsOnGetters(relativePath, source, errors);

  if (!type.name.endsWith("Entity")) {
    errors.push(`${relativePath}: las entidades JPA deben terminar en Entity.`);
  }
}

function validateJpaRelationsOnGetters(relativePath, source, errors) {
  const lines = source.split(/\r?\n/);

  for (let index = 0; index < lines.length; index += 1) {
    const annotation = readRelationAnnotation(lines[index]);

    if (!annotation) {
      continue;
    }

    const nextLine = readNextNonJpaAnnotationCodeLine(lines, index + 1);

    if (!nextLine || !/\bget[A-Z][A-Za-z0-9]*\s*\(/.test(nextLine.text)) {
      errors.push(
        `${relativePath}:${index + 1} @${annotation} debe declararse sobre un método get*.`,
      );
      continue;
    }

    const propertySuffix = nextLine.text.match(/\bget([A-Z][A-Za-z0-9]*)\s*\(/)?.[1];

    if (propertySuffix && !new RegExp(`\\bset${propertySuffix}\\s*\\(`).test(source)) {
      errors.push(
        `${relativePath}:${nextLine.lineNumber} la relación get${propertySuffix} debe tener set${propertySuffix}.`,
      );
    }
  }
}

function validateDao(relativePath, source, type, errors) {
  if (!type.name.endsWith("Dao")) {
    return;
  }

  if (!/\b@Repository\b/.test(source) && !/\bRepository<|\bJpaRepository</.test(source)) {
    errors.push(`${relativePath}: los DAOs deben ser repositorios Spring documentados.`);
  }

  const lines = source.split(/\r?\n/);

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index].trim();

    if (!isDaoMethodDeclaration(line)) {
      continue;
    }

    const previousLines = lines.slice(Math.max(0, index - 4), index).join("\n");

    if (!/@Query\b/.test(previousLines)) {
      errors.push(`${relativePath}:${index + 1} las consultas DAO propias deben declarar @Query.`);
    }
  }
}

function validateDtoAndConverterNames(relativePath, type, errors) {
  const normalizedPath = relativePath.replaceAll("\\", "/");
  const isDtoPackage = /\/dtos?\//u.test(normalizedPath);
  const isConverterPackage = /\/converters?\//u.test(normalizedPath);

  if (isDtoPackage && !/(Command|Dto|Request|Response)$/.test(type.name)) {
    errors.push(
      `${relativePath}: los DTOs REST deben terminar en Request, Response, Command o Dto.`,
    );
  }

  if (isConverterPackage && !type.name.endsWith("Converter")) {
    errors.push(`${relativePath}: los conversores explícitos deben terminar en Converter.`);
  }
}

async function validateMigrationFile(file, errors) {
  const source = await readFile(file, "utf8");
  const relativePath = toRelativePath(file);

  for (const match of source.matchAll(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([^\s(]+)/giu)) {
    validateQuotedUpperCamelIdentifier(relativePath, "tabla Flyway", match[1], errors);
  }

  for (const match of source.matchAll(/ALTER\s+TABLE\s+(?:ONLY\s+)?([^\s;]+)/giu)) {
    validateQuotedUpperCamelIdentifier(relativePath, "tabla Flyway", match[1], errors);
  }

  for (const column of readColumnDefinitions(source)) {
    validateQuotedLowerCamelIdentifier(relativePath, "columna Flyway", column, errors);
  }
}

function readColumnDefinitions(source) {
  const columns = [];
  const createTableStatements = source.matchAll(
    /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[^\s(]+\s*\(/giu,
  );

  for (const statement of createTableStatements) {
    const openingParenthesis = statement.index + statement[0].lastIndexOf("(");
    const body = readParenthesizedSql(source, openingParenthesis);

    for (const definition of splitTopLevelSqlDefinitions(body)) {
      const columnMatch = definition.trim().match(/^("[^"]+"|[A-Za-z_][A-Za-z0-9_]*)\s+/u);

      if (columnMatch && !/^(CONSTRAINT|PRIMARY|FOREIGN|UNIQUE|CHECK)\b/iu.test(columnMatch[1])) {
        columns.push(columnMatch[1]);
      }
    }
  }

  return columns;
}

/**
 * Extrae el cuerpo de unos paréntesis SQL respetando literales y comentarios.
 *
 * El parser anterior usaba una expresión regular no codiciosa y terminaba una tabla en el primer
 * `);` interno, por ejemplo el de una función PostGIS. Este lector pequeño no pretende analizar
 * todo SQL: únicamente encuentra el paréntesis de cierre estructural de CREATE TABLE.
 */
function readParenthesizedSql(source, openingParenthesis) {
  let depth = 0;
  let state = "code";

  for (let index = openingParenthesis; index < source.length; index += 1) {
    const character = source[index];
    const nextCharacter = source[index + 1];

    if (state === "lineComment") {
      if (character === "\n") {
        state = "code";
      }
      continue;
    }

    if (state === "blockComment") {
      if (character === "*" && nextCharacter === "/") {
        state = "code";
        index += 1;
      }
      continue;
    }

    if (state === "singleQuote") {
      if (character === "'" && nextCharacter === "'") {
        index += 1;
      } else if (character === "'") {
        state = "code";
      }
      continue;
    }

    if (state === "doubleQuote") {
      if (character === '"' && nextCharacter === '"') {
        index += 1;
      } else if (character === '"') {
        state = "code";
      }
      continue;
    }

    if (character === "-" && nextCharacter === "-") {
      state = "lineComment";
      index += 1;
    } else if (character === "/" && nextCharacter === "*") {
      state = "blockComment";
      index += 1;
    } else if (character === "'") {
      state = "singleQuote";
    } else if (character === '"') {
      state = "doubleQuote";
    } else if (character === "(") {
      depth += 1;
    } else if (character === ")") {
      depth -= 1;
      if (depth === 0) {
        return source.slice(openingParenthesis + 1, index);
      }
    }
  }

  return source.slice(openingParenthesis + 1);
}

/**
 * Separa columnas y constraints solo por comas del primer nivel del cuerpo de CREATE TABLE.
 * Las comas de tipos, funciones, checks o literales permanecen dentro de su definición.
 */
function splitTopLevelSqlDefinitions(body) {
  const definitions = [];
  let start = 0;
  let depth = 0;
  let state = "code";

  for (let index = 0; index < body.length; index += 1) {
    const character = body[index];
    const nextCharacter = body[index + 1];

    if (state === "lineComment") {
      if (character === "\n") {
        state = "code";
      }
      continue;
    }

    if (state === "blockComment") {
      if (character === "*" && nextCharacter === "/") {
        state = "code";
        index += 1;
      }
      continue;
    }

    if (state === "singleQuote") {
      if (character === "'" && nextCharacter === "'") {
        index += 1;
      } else if (character === "'") {
        state = "code";
      }
      continue;
    }

    if (state === "doubleQuote") {
      if (character === '"' && nextCharacter === '"') {
        index += 1;
      } else if (character === '"') {
        state = "code";
      }
      continue;
    }

    if (character === "-" && nextCharacter === "-") {
      state = "lineComment";
      index += 1;
    } else if (character === "/" && nextCharacter === "*") {
      state = "blockComment";
      index += 1;
    } else if (character === "'") {
      state = "singleQuote";
    } else if (character === '"') {
      state = "doubleQuote";
    } else if (character === "(") {
      depth += 1;
    } else if (character === ")") {
      depth -= 1;
    } else if (character === "," && depth === 0) {
      definitions.push(body.slice(start, index));
      start = index + 1;
    }
  }

  definitions.push(body.slice(start));
  return definitions;
}

async function readJavaTypes(files) {
  const types = new Map();

  for (const file of files) {
    const source = await readFile(file, "utf8");
    const type = readPrimaryJavaType(source);

    if (type) {
      types.set(type.name, {
        ...type,
        file,
      });
    }
  }

  return types;
}

function readPrimaryJavaType(source) {
  const match = source.match(/\b(public\s+)?(class|interface|enum|record)\s+([A-Z][A-Za-z0-9]*)/u);

  if (!match) {
    return undefined;
  }

  return {
    kind: match[2],
    name: match[3],
  };
}

function hasAnnotation(source, annotationName) {
  return new RegExp(`@${annotationName}\\b`, "u").test(source);
}

function readAnnotationName(source, annotationName) {
  return readAnnotationNames(source, annotationName)[0];
}

function readAnnotationNames(source, annotationName) {
  const names = [];
  const annotationPattern = new RegExp(`@${annotationName}\\s*\\(([^)]*)\\)`, "gu");

  for (const match of source.matchAll(annotationPattern)) {
    const nameMatch = match[1].match(/\bname\s*=\s*"((?:\\"|[^"])*)"/u);

    if (nameMatch) {
      names.push(nameMatch[1].replaceAll('\\"', '"'));
    }
  }

  return names;
}

function readRelationAnnotation(line) {
  const annotationMatch = line.match(/@(ManyToMany|ManyToOne|OneToMany|OneToOne)\b/u);
  const annotation = annotationMatch?.[1];

  return annotation && relationAnnotations.has(annotation) ? annotation : undefined;
}

function readNextNonJpaAnnotationCodeLine(lines, startIndex) {
  let annotationDepth = 0;

  for (let index = startIndex; index < lines.length; index += 1) {
    const text = lines[index].trim();

    if (!text || text.startsWith("//") || text.startsWith("*")) {
      continue;
    }

    if (annotationDepth > 0) {
      annotationDepth += countChar(text, "(") - countChar(text, ")");
      continue;
    }

    if (text.startsWith("@")) {
      annotationDepth = Math.max(0, countChar(text, "(") - countChar(text, ")"));
      continue;
    }

    return {
      lineNumber: index + 1,
      text,
    };
  }

  return undefined;
}

function countChar(value, char) {
  return [...value].filter((current) => current === char).length;
}

function isDaoMethodDeclaration(line) {
  return (
    line.endsWith(";") &&
    /\b[A-Za-z0-9_<>, ?]+\s+[a-z][A-Za-z0-9]*\s*\([^)]*\)\s*;$/u.test(line) &&
    !line.startsWith("default ") &&
    !line.startsWith("static ")
  );
}

function validateQuotedUpperCamelIdentifier(relativePath, label, value, errors) {
  const identifier = unwrapQuotedIdentifier(value);

  if (!isQuotedIdentifier(value) || !upperCamelCasePattern.test(identifier)) {
    errors.push(`${relativePath}: ${label} ${value} debe ser "\"UpperCamelCase\"".`);
  }
}

function validateQuotedLowerCamelIdentifier(relativePath, label, value, errors) {
  const identifier = unwrapQuotedIdentifier(value);

  if (!isQuotedIdentifier(value) || !lowerCamelCasePattern.test(identifier)) {
    errors.push(`${relativePath}: ${label} ${value} debe ser "\"lowerCamelCase\"".`);
  }
}

function isQuotedIdentifier(value) {
  return /^"[^"]+"$/u.test(value);
}

function unwrapQuotedIdentifier(value) {
  return value.replace(/^"/u, "").replace(/"$/u, "");
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

function toRelativePath(file) {
  return path.relative(fileURLToPath(repoRoot), file).replaceAll(path.sep, "/");
}
