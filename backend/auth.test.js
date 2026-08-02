const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const {
  authenticateRequest,
  createSession,
  hashPassword,
  verifyPassword
} = require("./auth");
const { JsonStore } = require("./store");

test("password credentials are salted, hashed, and verifiable", async () => {
  const password = "Responder#123";
  const first = await hashPassword(password);
  const second = await hashPassword(password);

  assert.equal(first.algorithm, "scrypt");
  assert.notEqual(first.salt, second.salt);
  assert.notEqual(first.hash, second.hash);
  assert.equal(JSON.stringify(first).includes(password), false);
  assert.equal(await verifyPassword(password, first), true);
  assert.equal(await verifyPassword("incorrect-password", first), false);
});

test("sessions persist only token hashes and expire", () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-auth-"));
  const filePath = path.join(directory, "data.json");
  const store = new JsonStore(filePath);
  store.upsert("users", {
    id: "user_1",
    email: "responder@example.com",
    role: "responder",
    status: "active"
  });

  const now = new Date("2026-07-13T00:00:00.000Z");
  const { token, session } = createSession(store, "user_1", { ttlMs: 1_000, now });
  const request = { headers: { authorization: `Bearer ${token}` } };
  const persisted = fs.readFileSync(filePath, "utf8");

  assert.equal(persisted.includes(token), false);
  assert.ok(persisted.includes(session.tokenHash));
  assert.equal(authenticateRequest(request, store, { now: "2026-07-13T00:00:00.500Z" }).user.id, "user_1");
  assert.equal(authenticateRequest(request, store, { now: "2026-07-13T00:00:01.001Z" }), null);
  assert.equal(store.list("sessions").length, 0);
});

test("audit records cannot be updated or removed", () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-audit-"));
  const store = new JsonStore(path.join(directory, "data.json"));
  const event = {
    id: "audit_1",
    actorUserId: "user_1",
    action: "incident.status_changed",
    timestamp: "2026-07-13T00:00:00.000Z"
  };

  store.append("audits", event);

  assert.throws(() => store.upsert("audits", { ...event, action: "changed" }), /append-only/);
  assert.throws(() => store.remove("audits", event.id), /append-only/);
  assert.throws(() => store.append("audits", event), /already exists/);
  assert.deepEqual(store.get("audits", event.id), event);
});
