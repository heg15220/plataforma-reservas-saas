import { readFile } from "node:fs/promises";

const workflowPath = new URL("../.github/workflows/ci.yml", import.meta.url);
const workflow = await readFile(workflowPath, "utf8");

const requiredFragments = [
  "pull_request:",
  "- develop",
  "- main",
  '- "phase/**"',
  "workflow_dispatch:",
  "permissions:\n  contents: read",
  "cancel-in-progress: true",
  "name: Quality",
  "name: Frontend",
  "name: Backend integration",
  "runs-on: ubuntu-24.04",
  "uses: actions/checkout@v7",
  "uses: actions/setup-node@v6",
  'node-version: "22"',
  "uses: actions/setup-java@v5",
  'java-version: "21"',
  "persist-credentials: false",
  "npm ci",
  "npm run format:check",
  "npm run lint",
  "npm run typecheck",
  "npm run test:web",
  "npm run build:web:test",
  "test -Dspring.profiles.active=test",
  "package -DskipTests",
];

const forbiddenFragments = [
  "pull_request_target:",
  "workflow_run:",
  "contents: write",
  "persist-credentials: true",
];

const errors = [];

if (workflow.includes("\t")) {
  errors.push("El workflow contiene tabuladores; YAML debe usar espacios.");
}

for (const fragment of requiredFragments) {
  if (!workflow.includes(fragment)) {
    errors.push(`Falta el contrato obligatorio: ${fragment}`);
  }
}

for (const fragment of forbiddenFragments) {
  if (workflow.includes(fragment)) {
    errors.push(`El workflow contiene una configuración prohibida: ${fragment}`);
  }
}

const jobTimeouts = workflow.match(/timeout-minutes:\s+\d+/g) ?? [];
if (jobTimeouts.length !== 3) {
  errors.push("Los tres jobs deben declarar timeout-minutes.");
}

if (errors.length > 0) {
  console.error("Contrato CI inválido:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exitCode = 1;
} else {
  console.log("Contrato CI válido: Quality, Frontend y Backend integration.");
}
