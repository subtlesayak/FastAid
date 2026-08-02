const assert = require("node:assert/strict");
const test = require("node:test");

const { evaluatePilotReadiness, isHttpsUrl } = require("./pilotReadiness");

test("local readiness requires storage but treats deployment integrations as optional", () => {
  const ready = evaluatePilotReadiness({
    mode: "local",
    storageReady: true,
    notificationProvider: "local"
  });
  const blocked = evaluatePilotReadiness({
    mode: "local",
    storageReady: false,
    notificationProvider: "local"
  });

  assert.equal(ready.ready, true);
  assert.equal(ready.checks.publicUrl.required, false);
  assert.equal(ready.checks.notifications.mode, "simulated");
  assert.equal(blocked.ready, false);
});

test("pilot readiness requires HTTPS, Maps, admin, storage, and a usable notification mode", () => {
  const missing = evaluatePilotReadiness({
    mode: "pilot",
    storageReady: true,
    notificationProvider: "firebase",
    firebaseAdapterReady: false
  });
  const configured = evaluatePilotReadiness({
    mode: "pilot",
    storageReady: true,
    publicBaseUrl: "https://api.example.org",
    mapsKeyPresent: true,
    adminConfigured: true,
    notificationProvider: "local"
  });

  assert.equal(missing.ready, false);
  assert.equal(missing.checks.notifications.ready, false);
  assert.equal(configured.ready, true);
  assert.ok(configured.warnings.some((warning) => warning.includes("simulated")));
});

test("accepts only HTTPS public origins", () => {
  assert.equal(isHttpsUrl("https://api.example.org"), true);
  assert.equal(isHttpsUrl("http://api.example.org"), false);
  assert.equal(isHttpsUrl("not-a-url"), false);
});
