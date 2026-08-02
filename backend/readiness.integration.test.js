const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const net = require("node:net");
const os = require("node:os");
const path = require("node:path");
const fs = require("node:fs");
const test = require("node:test");

test("pilot readiness returns 503 for missing deployment requirements", { timeout: 20_000 }, async () => {
  const harness = await startBackend({ mode: "pilot" });
  try {
    const health = await request(harness.port, "/api/health");
    const readiness = await request(harness.port, "/api/readiness");

    assert.equal(health.status, 200);
    assert.equal(health.payload.ok, true);
    assert.equal(readiness.status, 503);
    assert.equal(readiness.payload.ready, false);
    assert.equal(readiness.payload.checks.publicUrl.ready, false);
    assert.equal(readiness.payload.checks.maps.ready, false);
  } finally {
    await stopBackend(harness.child);
  }
});

test("pilot readiness returns 200 when controlled-pilot requirements are configured", { timeout: 20_000 }, async () => {
  const harness = await startBackend({
    mode: "pilot",
    publicBaseUrl: "https://api.fastaid.test",
    mapsKey: "test-server-key-not-for-production",
    adminEmail: "readiness-admin@fastaid.test",
    adminPassword: "ReadinessAdmin#123"
  });
  try {
    const readiness = await request(harness.port, "/api/readiness");

    assert.equal(readiness.status, 200);
    assert.equal(readiness.payload.ready, true);
    assert.equal(readiness.payload.mode, "pilot");
    assert.equal(readiness.payload.checks.notifications.mode, "simulated");
  } finally {
    await stopBackend(harness.child);
  }
});

async function startBackend(options) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-readiness-"));
  const port = await availablePort();
  const child = spawn(process.execPath, [path.join(__dirname, "server.js")], {
    cwd: path.resolve(__dirname, ".."),
    env: {
      ...process.env,
      FASTAID_SKIP_DOTENV: "1",
      PORT: String(port),
      FASTAID_DATA_FILE: path.join(directory, "pilot-data.json"),
      FASTAID_DEPLOYMENT_MODE: options.mode || "local",
      FASTAID_PUBLIC_BASE_URL: options.publicBaseUrl || "",
      FASTAID_ADMIN_EMAIL: options.adminEmail || "",
      FASTAID_ADMIN_PASSWORD: options.adminPassword || "",
      FASTAID_NOTIFICATION_PROVIDER: "local",
      GOOGLE_MAPS_SERVER_KEY: options.mapsKey || ""
    },
    stdio: ["ignore", "pipe", "pipe"]
  });
  await waitForHealth(port);
  return { child, port };
}

async function stopBackend(child) {
  if (!child || child.exitCode !== null) return;
  child.kill();
  await new Promise((resolve) => {
    child.once("exit", resolve);
    setTimeout(resolve, 2_000);
  });
}

async function waitForHealth(port) {
  const deadline = Date.now() + 12_000;
  while (Date.now() < deadline) {
    try {
      const health = await request(port, "/api/health");
      if (health.status === 200) return;
    } catch (error) {
      // The optional admin bootstrap can delay server listen briefly.
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error("FastAid readiness test backend did not become healthy");
}

async function request(port, pathname) {
  const response = await fetch(`http://127.0.0.1:${port}${pathname}`);
  return { status: response.status, payload: await response.json() };
}

function availablePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      const { port } = server.address();
      server.close((error) => error ? reject(error) : resolve(port));
    });
  });
}
