const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const sensitiveNames = new Set([
  "GOOGLE_MAPS_SERVER_KEY",
  "FASTAID_ADMIN_PASSWORD",
  "FCM_SERVER_KEY",
  "SMS_PROVIDER_API_KEY"
]);
const excludedDirectories = new Set([
  ".git",
  ".gradle",
  ".idea",
  ".codex-remote-attachments",
  "node_modules",
  "data"
]);
const sourceExtensions = new Set([
  ".cjs", ".gradle", ".java", ".js", ".json", ".md", ".properties", ".xml", ".yaml", ".yml"
]);

const secrets = loadConfiguredSecrets();
const findings = [];
for (const filePath of walk(root)) {
  const relative = path.relative(root, filePath).replaceAll("\\", "/");
  if ([".env", ".env.local", "android/local.properties"].includes(relative)) continue;
  const extension = path.extname(filePath).toLowerCase();
  const isApk = extension === ".apk" || extension === ".aab";
  if (!isApk && /(^|\/)build\//.test(relative)) continue;
  if (!isApk && !sourceExtensions.has(extension)) continue;
  const bytes = fs.readFileSync(filePath);
  for (const secret of secrets) {
    if (bytes.includes(Buffer.from(secret.value))) {
      findings.push({ name: secret.name, file: relative, artifact: isApk });
    }
  }
}

for (const tracked of trackedFiles()) {
  if (tracked === ".env" || tracked === ".env.local" || tracked === "android/local.properties") {
    findings.push({ name: "tracked-secret-file", file: tracked, artifact: false });
  }
  if (/^backend\/data\/.*\.(json|tmp)$/i.test(tracked)) {
    findings.push({ name: "tracked-pilot-data", file: tracked, artifact: false });
  }
}

if (findings.length) {
  console.error("Pilot artifact verification failed. Sensitive values or data were found:");
  for (const finding of findings) {
    console.error(`- ${finding.name} in ${finding.file}${finding.artifact ? " (packaged artifact)" : ""}`);
  }
  process.exitCode = 1;
} else {
  console.log(`Pilot artifact verification passed (${secrets.length} configured server secrets checked).`);
}

function loadConfiguredSecrets() {
  const values = [];
  for (const name of [".env", ".env.local"]) {
    const filePath = path.join(root, name);
    if (!fs.existsSync(filePath)) continue;
    for (const line of fs.readFileSync(filePath, "utf8").split(/\r?\n/)) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith("#") || !trimmed.includes("=")) continue;
      const index = trimmed.indexOf("=");
      const key = trimmed.slice(0, index).trim();
      const value = trimmed.slice(index + 1).trim().replace(/^["']|["']$/g, "");
      if (sensitiveNames.has(key) && value.length >= 8) values.push({ name: key, value });
    }
  }
  return values;
}

function walk(directory) {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.isDirectory() && excludedDirectories.has(entry.name)) continue;
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...walk(entryPath));
    else if (entry.isFile()) files.push(entryPath);
  }
  return files;
}

function trackedFiles() {
  try {
    return execFileSync("git", ["ls-files"], { cwd: root, encoding: "utf8" })
      .split(/\r?\n/)
      .filter(Boolean)
      .map((file) => file.replaceAll("\\", "/"));
  } catch (error) {
    return [];
  }
}
