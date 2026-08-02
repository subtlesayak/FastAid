const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const fs = require("node:fs");
const net = require("node:net");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

test("decline advances to the next ranked responder with one active alert", { timeout: 30_000 }, async () => {
  const harness = await startHarness({ alertTimeoutSeconds: 10 });
  try {
    const near = await createVerifiedResponder(harness, {
      suffix: "decline-near",
      name: "Near Repair",
      lat: 28.6329
    });
    const far = await createVerifiedResponder(harness, {
      suffix: "decline-far",
      name: "Far Repair",
      lat: 28.635
    });

    const created = await request(harness.port, "/api/incidents", {
      method: "POST",
      body: incidentBody()
    });
    assert.equal(created.status, 201);
    assert.equal(created.payload.alert.responderId, near.id);
    assert.equal(created.payload.alert.notificationStatus, "delivered");

    const declined = await request(
      harness.port,
      `/api/responder-alerts/${created.payload.alert.id}/decline`,
      { method: "POST", token: near.token }
    );
    assert.equal(declined.status, 200);
    assert.equal(declined.payload.alert.status, "declined");
    assert.equal(declined.payload.nextAlert.responderId, far.id);
    assert.equal(declined.payload.nextAlert.attemptNumber, 2);

    const persisted = JSON.parse(fs.readFileSync(harness.dataFile, "utf8"));
    const incidentAlerts = persisted.alerts.filter((alert) => alert.incidentId === created.payload.incident.id);
    const active = incidentAlerts.filter((alert) => ["notified", "accepted"].includes(alert.status));
    assert.equal(active.length, 1);
    assert.equal(active[0].responderId, far.id);

    const notifications = await request(harness.port, "/api/admin/notifications", {
      token: harness.adminToken
    });
    assert.equal(notifications.status, 200);
    const incidentNotifications = notifications.payload.attempts.filter(
      (attempt) => attempt.incidentId === created.payload.incident.id
    );
    assert.equal(incidentNotifications.length, 2);
    assert.ok(incidentNotifications.every((attempt) => attempt.status === "delivered"));

    const accepted = await request(
      harness.port,
      `/api/responder-alerts/${declined.payload.nextAlert.id}/accept`,
      { method: "POST", token: far.token }
    );
    assert.equal(accepted.status, 200);
    assert.equal(accepted.payload.incident.status, "en_route");

    const skipped = await request(harness.port, `/api/incidents/${created.payload.incident.id}`, {
      method: "PATCH",
      token: harness.adminToken,
      body: { status: "resolved" }
    });
    assert.equal(skipped.status, 409);

    const arrived = await request(harness.port, `/api/incidents/${created.payload.incident.id}`, {
      method: "PATCH",
      token: harness.adminToken,
      body: { status: "arrived" }
    });
    assert.equal(arrived.status, 200);
    const resolved = await request(harness.port, `/api/incidents/${created.payload.incident.id}`, {
      method: "PATCH",
      token: harness.adminToken,
      body: { status: "resolved" }
    });
    assert.equal(resolved.status, 200);
    const responders = await request(harness.port, "/api/responders", { token: harness.adminToken });
    const released = responders.payload.responders.find((responder) => responder.id === far.id);
    assert.equal(released.availabilityStatus, "available");
  } finally {
    await stopBackend(harness.child);
  }
});

test("timeout releases the first responder and advances to the next", { timeout: 30_000 }, async () => {
  const harness = await startHarness({ alertTimeoutSeconds: 1 });
  try {
    const near = await createVerifiedResponder(harness, {
      suffix: "timeout-near",
      name: "Timeout Near Repair",
      lat: 28.6329
    });
    const far = await createVerifiedResponder(harness, {
      suffix: "timeout-far",
      name: "Timeout Far Repair",
      lat: 28.635
    });

    const created = await request(harness.port, "/api/incidents", {
      method: "POST",
      body: incidentBody()
    });
    assert.equal(created.status, 201);
    assert.equal(created.payload.alert.responderId, near.id);

    const rollover = await waitFor(async () => {
      const firstAlerts = await request(harness.port, `/api/responders/${near.id}/alerts`, {
        token: harness.adminToken
      });
      const secondAlerts = await request(harness.port, `/api/responders/${far.id}/alerts`, {
        token: harness.adminToken
      });
      const timedOut = firstAlerts.payload.alerts.find((alert) => alert.id === created.payload.alert.id);
      const next = secondAlerts.payload.alerts.find((alert) => alert.incidentId === created.payload.incident.id);
      return timedOut?.status === "timed_out" && next?.status === "notified"
        ? { timedOut, next }
        : null;
    }, 5_000);

    assert.equal(rollover.timedOut.responderId, near.id);
    assert.equal(rollover.next.responderId, far.id);
    assert.equal(rollover.next.attemptNumber, 2);

    const persisted = JSON.parse(fs.readFileSync(harness.dataFile, "utf8"));
    const active = persisted.alerts.filter((alert) => (
      alert.incidentId === created.payload.incident.id
      && ["notified", "accepted"].includes(alert.status)
    ));
    assert.equal(active.length, 1);
    assert.equal(active[0].responderId, far.id);
  } finally {
    await stopBackend(harness.child);
  }
});

async function startHarness({ alertTimeoutSeconds }) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-matching-server-"));
  const dataFile = path.join(directory, "pilot-data.json");
  const port = await availablePort();
  const adminEmail = `admin-${port}@fastaid.test`;
  const adminPassword = "AdminPilot#123";
  const child = startBackend(port, dataFile, {
    adminEmail,
    adminPassword,
    alertTimeoutSeconds
  });
  await waitForHealth(port);
  const login = await request(port, "/api/auth/login", {
    method: "POST",
    body: { email: adminEmail, password: adminPassword }
  });
  assert.equal(login.status, 200);
  return { child, dataFile, port, adminToken: login.payload.token };
}

async function createVerifiedResponder(harness, { suffix, name, lat }) {
  const password = "Responder#123";
  const signup = await request(harness.port, "/api/auth/signup", {
    method: "POST",
    body: {
      email: `${suffix}@fastaid.test`,
      password,
      name,
      role: "responder"
    }
  });
  assert.equal(signup.status, 201);
  const token = signup.payload.token;
  const profile = await request(harness.port, "/api/responders", {
    method: "POST",
    token,
    body: {
      name,
      responderType: "car_repair",
      location: { lat, lng: 77.2197 },
      serviceRadiusKm: 20
    }
  });
  assert.equal(profile.status, 201);
  const id = profile.payload.responder.id;
  const verified = await request(harness.port, `/api/admin/responders/${id}/verification`, {
    method: "PATCH",
    token: harness.adminToken,
    body: { status: "verified" }
  });
  assert.equal(verified.status, 200);
  const available = await request(harness.port, `/api/responders/${id}/availability`, {
    method: "PATCH",
    token,
    body: { status: "available" }
  });
  assert.equal(available.status, 200);
  return { id, token };
}

function incidentBody() {
  return {
    type: "Breakdown",
    peopleCount: 1,
    locationText: "Matching integration test",
    location: { lat: 28.6328, lng: 77.2197 }
  };
}

function startBackend(port, dataFile, { adminEmail, adminPassword, alertTimeoutSeconds }) {
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
      FASTAID_ALERT_TIMEOUT_SECONDS: String(alertTimeoutSeconds),
      FASTAID_ALERT_SWEEP_INTERVAL_MS: "50",
      FASTAID_NOTIFICATION_PROVIDER: "local",
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
      // The optional administrator bootstrap hashes its password before listen.
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error("FastAid matching test backend did not become healthy");
}

async function waitFor(check, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const value = await check();
    if (value) return value;
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
  throw new Error("Timed out waiting for responder rollover");
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
