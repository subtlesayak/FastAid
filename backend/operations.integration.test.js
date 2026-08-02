const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const fs = require("node:fs");
const net = require("node:net");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

test("operations console APIs require admin access and keep duplicate review non-destructive", { timeout: 30_000 }, async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-operations-"));
  const dataFile = path.join(directory, "pilot-data.json");
  const port = await availablePort();
  const adminEmail = "operations-admin@fastaid.test";
  const adminPassword = "Operations#123";
  const child = startBackend(port, dataFile, { adminEmail, adminPassword });

  try {
    await waitForHealth(port);

    const consolePage = await fetch(`http://127.0.0.1:${port}/operations.html`);
    assert.equal(consolePage.status, 200);
    assert.match(await consolePage.text(), /Administrator sign in/);

    const anonymousDuplicates = await request(port, "/api/admin/duplicate-suggestions");
    assert.equal(anonymousDuplicates.status, 401);

    const userSignup = await request(port, "/api/auth/signup", {
      method: "POST",
      body: {
        email: "pilot-user@fastaid.test",
        password: "PilotUser#123",
        name: "Pilot User",
        role: "user"
      }
    });
    assert.equal(userSignup.status, 201);
    const userDuplicates = await request(port, "/api/admin/duplicate-suggestions", {
      token: userSignup.payload.token
    });
    assert.equal(userDuplicates.status, 403);

    const adminLogin = await request(port, "/api/auth/login", {
      method: "POST",
      body: { email: adminEmail, password: adminPassword }
    });
    assert.equal(adminLogin.status, 200);
    const adminToken = adminLogin.payload.token;

    const responders = await request(port, "/api/responders", { token: adminToken });
    assert.equal(responders.status, 200);
    const responder = responders.payload.responders[0];
    assert.ok(responder);

    const suspension = await request(
      port,
      `/api/admin/responders/${encodeURIComponent(responder.id)}/verification`,
      {
        method: "PATCH",
        token: adminToken,
        body: { status: "suspended", reason: "Pilot credential review" }
      }
    );
    assert.equal(suspension.status, 200);
    assert.equal(suspension.payload.responder.verificationStatus, "suspended");
    assert.equal(suspension.payload.responder.availabilityStatus, "offline");

    const firstIncident = await createIncident(port, {
      type: "Accident",
      locationText: "MG Road Metro",
      location: { lat: 28.6328, lng: 77.2197 }
    });
    const secondIncident = await createIncident(port, {
      type: "Accident",
      locationText: "MG Road Metro",
      location: { lat: 28.633, lng: 77.2199 }
    });
    assert.equal(firstIncident.status, 201);
    assert.equal(secondIncident.status, 201);

    const incidentsBefore = await request(port, "/api/incidents", { token: adminToken });
    const statusesBefore = new Map(incidentsBefore.payload.incidents.map((incident) => [incident.id, incident.status]));

    const duplicates = await request(port, "/api/admin/duplicate-suggestions", { token: adminToken });
    assert.equal(duplicates.status, 200);
    assert.equal(duplicates.payload.policy.reviewOnly, true);
    assert.equal(duplicates.payload.policy.automaticMerge, false);
    assert.ok(duplicates.payload.suggestions.some((suggestion) => (
      suggestion.incidentIds.includes(firstIncident.payload.incident.id)
      && suggestion.incidentIds.includes(secondIncident.payload.incident.id)
    )));

    const incidentsAfter = await request(port, "/api/incidents", { token: adminToken });
    for (const incident of incidentsAfter.payload.incidents) {
      assert.equal(incident.status, statusesBefore.get(incident.id));
    }

    const audit = await request(port, "/api/admin/audit", { token: adminToken });
    const verificationEvent = audit.payload.events.find((event) => (
      event.action === "responder.verification_changed" && event.targetId === responder.id
    ));
    assert.equal(verificationEvent.details.reason, "Pilot credential review");
  } finally {
    await stopBackend(child);
  }
});

function createIncident(port, overrides) {
  return request(port, "/api/incidents", {
    method: "POST",
    body: {
      peopleCount: 2,
      note: "Controlled operations test",
      ...overrides
    }
  });
}

function startBackend(port, dataFile, { adminEmail, adminPassword }) {
  return spawn(process.execPath, [path.join(__dirname, "server.js")], {
    cwd: path.resolve(__dirname, ".."),
    env: {
      ...process.env,
      FASTAID_SKIP_DOTENV: "1",
      PORT: String(port),
      FASTAID_DATA_FILE: dataFile,
      FASTAID_ADMIN_EMAIL: adminEmail,
      FASTAID_ADMIN_PASSWORD: adminPassword,
      FASTAID_SESSION_TTL_MINUTES: "10",
      FASTAID_ALERT_TIMEOUT_SECONDS: "60",
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
  const deadline = Date.now() + 12_000;
  while (Date.now() < deadline) {
    try {
      const health = await request(port, "/api/health");
      if (health.status === 200 && health.payload.ok) return;
    } catch (error) {
      // Startup may include password hashing for the administrator bootstrap.
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error("FastAid operations test backend did not become healthy");
}

async function request(port, pathname, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.token) headers.authorization = `Bearer ${options.token}`;
  if (options.body !== undefined) headers["content-type"] = "application/json";
  const response = await fetch(`http://127.0.0.1:${port}${pathname}`, {
    method: options.method || "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });
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
