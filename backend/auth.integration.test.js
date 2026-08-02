const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const fs = require("node:fs");
const net = require("node:net");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

test("responder and admin operations enforce sessions, ownership, and audit", { timeout: 30_000 }, async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-auth-server-"));
  const dataFile = path.join(directory, "pilot-data.json");
  const port = await availablePort();
  const adminEmail = "pilot-admin@fastaid.test";
  const adminPassword = "AdminPilot#123";
  const responderPassword = "Responder#123";
  const child = startBackend(port, dataFile, { adminEmail, adminPassword });

  try {
    await waitForHealth(port);

    const forbiddenAdminSignup = await request(port, "/api/auth/signup", {
      method: "POST",
      body: {
        email: "public-admin@fastaid.test",
        password: "NotAllowed#123",
        name: "Public Admin",
        role: "admin"
      }
    });
    assert.equal(forbiddenAdminSignup.status, 400);

    const signup = await request(port, "/api/auth/signup", {
      method: "POST",
      body: {
        email: "responder-one@fastaid.test",
        password: responderPassword,
        name: "Responder One",
        role: "responder"
      }
    });
    assert.equal(signup.status, 201);
    assert.equal(signup.payload.user.role, "responder");
    assert.equal("passwordCredential" in signup.payload.user, false);
    const responderToken = signup.payload.token;

    const profile = await request(port, "/api/responders", {
      method: "POST",
      token: responderToken,
      body: {
        name: "Responder One Ambulance",
        responderType: "hospital",
        phone: "+91 9000000001",
        location: { lat: 28.6328, lng: 77.2197 }
      }
    });
    assert.equal(profile.status, 201);
    assert.equal(profile.payload.responder.verificationStatus, "pending");
    const responderId = profile.payload.responder.id;

    const unauthenticatedAvailability = await request(
      port,
      `/api/responders/${responderId}/availability`,
      { method: "PATCH", body: { status: "available" } }
    );
    assert.equal(unauthenticatedAvailability.status, 401);

    const pendingAvailability = await request(
      port,
      `/api/responders/${responderId}/availability`,
      { method: "PATCH", token: responderToken, body: { status: "available" } }
    );
    assert.equal(pendingAvailability.status, 403);

    const adminLogin = await request(port, "/api/auth/login", {
      method: "POST",
      body: { email: adminEmail, password: adminPassword }
    });
    assert.equal(adminLogin.status, 200);
    assert.equal(adminLogin.payload.user.role, "admin");
    const adminToken = adminLogin.payload.token;

    const verification = await request(
      port,
      `/api/admin/responders/${responderId}/verification`,
      { method: "PATCH", token: adminToken, body: { status: "verified" } }
    );
    assert.equal(verification.status, 200);
    assert.equal(verification.payload.responder.verificationStatus, "verified");

    const available = await request(
      port,
      `/api/responders/${responderId}/availability`,
      {
        method: "PATCH",
        token: responderToken,
        body: { status: "available", location: { lat: 28.633, lng: 77.22 } }
      }
    );
    assert.equal(available.status, 200);
    assert.equal(available.payload.responder.availabilityStatus, "available");

    const secondSignup = await request(port, "/api/auth/signup", {
      method: "POST",
      body: {
        email: "responder-two@fastaid.test",
        password: "Responder#456",
        name: "Responder Two",
        role: "responder"
      }
    });
    const secondProfile = await request(port, "/api/responders", {
      method: "POST",
      token: secondSignup.payload.token,
      body: { name: "Responder Two", responderType: "police" }
    });
    assert.equal(secondProfile.status, 201);

    const crossAccountAlerts = await request(port, `/api/responders/${responderId}/alerts`, {
      token: secondSignup.payload.token
    });
    assert.equal(crossAccountAlerts.status, 403);

    const audit = await request(port, "/api/admin/audit", { token: adminToken });
    assert.equal(audit.status, 200);
    const actions = audit.payload.events.map((event) => event.action);
    assert.ok(actions.includes("responder.profile_created"));
    assert.ok(actions.includes("responder.verification_changed"));
    assert.ok(actions.includes("responder.availability_changed"));

    const storedText = fs.readFileSync(dataFile, "utf8");
    assert.equal(storedText.includes(adminPassword), false);
    assert.equal(storedText.includes(responderPassword), false);
    assert.equal(storedText.includes(adminToken), false);
    assert.equal(storedText.includes(responderToken), false);

    const logout = await request(port, "/api/auth/logout", {
      method: "POST",
      token: responderToken
    });
    assert.equal(logout.status, 200);
    const revokedSession = await request(port, "/api/me", { token: responderToken });
    assert.equal(revokedSession.status, 401);
  } finally {
    await stopBackend(child);
  }
});

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
      // Startup includes password hashing for the optional admin bootstrap.
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error("FastAid auth test backend did not become healthy");
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
