const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const fs = require("node:fs");
const net = require("node:net");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const adminEmail = "restart-admin@fastaid.test";
const adminPassword = "RestartAdmin#123";

test("incident survives a backend restart", { timeout: 20_000 }, async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-server-"));
  const dataFile = path.join(directory, "pilot-data.json");
  const port = await availablePort();
  let child = startBackend(port, dataFile);

  try {
    await waitForHealth(port);
    const created = await requestJson(`http://127.0.0.1:${port}/api/incidents`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "Accident",
        peopleCount: 2,
        locationText: "Integration test location",
        location: { lat: 28.6328, lng: 77.2197 }
      })
    });

    assert.match(created.incident.id, /^inc_/);
    assert.ok(created.incident.createdAt);
    assert.ok(created.incident.updatedAt);
    assert.equal(created.alert.incidentId, created.incident.id);
    assert.ok(created.alert.responderId);
    await stopBackend(child);

    child = startBackend(port, dataFile);
    await waitForHealth(port);
    const login = await requestJson(`http://127.0.0.1:${port}/api/auth/login`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ email: adminEmail, password: adminPassword })
    });
    const listed = await requestJson(`http://127.0.0.1:${port}/api/incidents`, {
      headers: { authorization: `Bearer ${login.token}` }
    });
    assert.ok(listed.incidents.some((incident) => incident.id === created.incident.id));

    const authorization = { authorization: `Bearer ${login.token}` };
    const retrieved = await requestJson(`http://127.0.0.1:${port}/api/incidents/${created.incident.id}`, {
      headers: authorization
    });
    assert.equal(retrieved.incident.id, created.incident.id);

    const responders = await requestJson(`http://127.0.0.1:${port}/api/responders`, {
      headers: authorization
    });
    assert.ok(responders.responders.some((responder) => responder.id === created.alert.responderId));

    const alerts = await requestJson(
      `http://127.0.0.1:${port}/api/responders/${created.alert.responderId}/alerts`,
      { headers: authorization }
    );
    assert.ok(alerts.alerts.some((alert) => (
      alert.id === created.alert.id && alert.incidentId === created.incident.id
    )));

    const resolved = await requestJson(`http://127.0.0.1:${port}/api/incidents/${created.incident.id}`, {
      method: "PATCH",
      headers: { ...authorization, "content-type": "application/json" },
      body: JSON.stringify({ status: "cancelled" })
    });
    assert.equal(resolved.incident.status, "cancelled");
    assert.ok(new Date(resolved.incident.updatedAt) >= new Date(created.incident.updatedAt));
  } finally {
    await stopBackend(child);
  }
});

function startBackend(port, dataFile) {
  return spawn(process.execPath, [path.join(__dirname, "server.js")], {
    cwd: path.resolve(__dirname, ".."),
    env: {
      ...process.env,
      FASTAID_SKIP_DOTENV: "1",
      PORT: String(port),
      FASTAID_DATA_FILE: dataFile,
      FASTAID_ADMIN_EMAIL: adminEmail,
      FASTAID_ADMIN_PASSWORD: adminPassword,
      GOOGLE_MAPS_SERVER_KEY: ""
    },
    stdio: ["ignore", "pipe", "pipe"]
  });
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
  const deadline = Date.now() + 8_000;
  while (Date.now() < deadline) {
    try {
      const health = await requestJson(`http://127.0.0.1:${port}/api/health`);
      if (health.ok) return;
    } catch (error) {
      // Server startup can take a few polling attempts.
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error("FastAid test backend did not become healthy");
}

async function requestJson(url, options) {
  const response = await fetch(url, options);
  const payload = await response.json();
  if (!response.ok) throw new Error(`${response.status}: ${JSON.stringify(payload)}`);
  return payload;
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
