const http = require("node:http");
const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { aidPlaces, incidentToTypes } = require("./mockData");
const { JsonStore } = require("./store");
const {
  authenticateRequest,
  createSession,
  hashPassword,
  normalizeEmail,
  sanitizeUser,
  verifyPassword
} = require("./auth");
const {
  estimateEta: estimateResponderEta,
  formatDistance: formatResponderDistance,
  rankResponders
} = require("./matching");
const { createNotificationAdapter, deliverWithRetry } = require("./notifications");
const { evaluatePilotReadiness } = require("./pilotReadiness");
const { FixedWindowRateLimiter } = require("./rateLimit");
const { findDuplicateIncidentSuggestions } = require("./duplicates");

const rootDir = path.resolve(__dirname, "..");
const prototypeDir = path.join(rootDir, "prototype");
const countryConfigPath = path.join(prototypeDir, "country-config.json");
const countryConfig = JSON.parse(fs.readFileSync(countryConfigPath, "utf8"));

if (process.env.FASTAID_SKIP_DOTENV !== "1") {
  loadDotEnv(path.join(rootDir, ".env"));
  loadDotEnv(path.join(rootDir, ".env.local"));
}

const port = Number(process.env.PORT || 4173);
const dataFile = process.env.FASTAID_DATA_FILE || path.join(__dirname, "data", "fastaid-store.json");
const store = new JsonStore(dataFile);
const incidentStatuses = new Set([
  "created", "verifying", "notified", "accepted", "en_route", "arrived",
  "resolved", "cancelled", "failed", "searching_next_responder"
]);
const incidentTransitions = new Map([
  ["created", new Set(["verifying", "notified", "cancelled", "failed"])],
  ["verifying", new Set(["notified", "cancelled", "failed"])],
  ["notified", new Set(["accepted", "en_route", "searching_next_responder", "cancelled", "failed"])],
  ["accepted", new Set(["en_route", "cancelled", "failed"])],
  ["en_route", new Set(["arrived", "cancelled", "failed"])],
  ["arrived", new Set(["resolved", "failed"])],
  ["searching_next_responder", new Set(["notified", "cancelled", "failed"])],
  ["resolved", new Set()],
  ["cancelled", new Set()],
  ["failed", new Set()]
]);
const publicSignupRoles = new Set(["user", "responder"]);
const responderVerificationStatuses = new Set(["pending", "verified", "rejected", "suspended"]);
const configuredSessionTtlMinutes = Number(process.env.FASTAID_SESSION_TTL_MINUTES || 60);
const sessionTtlMinutes = Number.isFinite(configuredSessionTtlMinutes)
  ? Math.max(5, Math.min(1440, configuredSessionTtlMinutes))
  : 60;
const sessionTtlMs = sessionTtlMinutes * 60_000;
const configuredAlertTimeoutSeconds = Number(process.env.FASTAID_ALERT_TIMEOUT_SECONDS || 45);
const alertTimeoutMs = Math.max(
  100,
  (Number.isFinite(configuredAlertTimeoutSeconds) ? configuredAlertTimeoutSeconds : 45) * 1000
);
const configuredSweepIntervalMs = Number(process.env.FASTAID_ALERT_SWEEP_INTERVAL_MS || 1000);
const alertSweepIntervalMs = Math.max(
  50,
  Math.min(60_000, Number.isFinite(configuredSweepIntervalMs) ? configuredSweepIntervalMs : 1000)
);
const configuredNotificationAttempts = Number(process.env.FASTAID_NOTIFICATION_MAX_ATTEMPTS || 3);
const notificationMaxAttempts = Math.max(
  1,
  Math.min(5, Number.isFinite(configuredNotificationAttempts) ? configuredNotificationAttempts : 3)
);
const notificationAdapter = createNotificationAdapter({
  provider: String(process.env.FASTAID_NOTIFICATION_PROVIDER || "local").trim().toLowerCase()
});
const deploymentMode = String(process.env.FASTAID_DEPLOYMENT_MODE || "local").trim().toLowerCase() === "pilot"
  ? "pilot"
  : "local";
const publicBaseUrl = String(process.env.FASTAID_PUBLIC_BASE_URL || "").trim();
const activeAlertStatuses = new Set(["notified", "accepted"]);
const trustProxy = process.env.FASTAID_TRUST_PROXY === "1";
const authRateLimiter = new FixedWindowRateLimiter({
  limit: Number(process.env.FASTAID_AUTH_RATE_LIMIT || 20),
  windowMs: Number(process.env.FASTAID_AUTH_RATE_WINDOW_MS || 300_000)
});
const incidentRateLimiter = new FixedWindowRateLimiter({
  limit: Number(process.env.FASTAID_INCIDENT_RATE_LIMIT || 10),
  windowMs: Number(process.env.FASTAID_INCIDENT_RATE_WINDOW_MS || 60_000)
});
let alertSweepInProgress = false;

store.seedResponders(aidPlaces
  .filter((place) => place.verified)
  .map((place) => ({
    id: `responder_${place.id}`,
    name: place.name,
    responderType: place.type,
    phone: place.phone || "",
    verificationStatus: "verified",
    availabilityStatus: "available",
    location: place.location,
    serviceRadiusKm: 20,
    source: "prototype_seed"
  })));

function loadDotEnv(filePath) {
  if (!fs.existsSync(filePath)) return;
  const text = fs.readFileSync(filePath, "utf8");
  for (const line of text.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#") || !trimmed.includes("=")) continue;
    const index = trimmed.indexOf("=");
    const key = trimmed.slice(0, index).trim();
    const value = trimmed.slice(index + 1).trim().replace(/^["']|["']$/g, "");
    if (!process.env[key]) process.env[key] = value;
  }
}

function sendJson(res, status, body) {
  const json = JSON.stringify(body, null, 2);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store"
  });
  res.end(json);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.on("data", (chunk) => {
      body += chunk;
      if (body.length > 1_000_000) {
        req.destroy();
        reject(new Error("Request body too large"));
      }
    });
    req.on("end", () => {
      if (!body) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(body));
      } catch (error) {
        reject(error);
      }
    });
  });
}

function getClientAddress(req) {
  if (trustProxy) {
    const forwarded = String(req.headers["x-forwarded-for"] || "").split(",")[0].trim();
    if (forwarded) return forwarded;
  }
  return req.socket.remoteAddress || "unknown";
}

function enforceRateLimit(req, res, limiter, bucket) {
  const result = limiter.consume(`${bucket}:${getClientAddress(req)}`);
  if (result.allowed) return true;
  sendJson(res, 429, {
    error: "Too many requests",
    retryAfterSeconds: result.retryAfterSeconds
  });
  return false;
}

function findUserByEmail(email) {
  const normalized = normalizeEmail(email);
  return store.list("users").find((user) => user.email === normalized) || null;
}

function appendAudit(actor, action, targetType, targetId, details = {}) {
  const event = {
    id: `audit_${crypto.randomUUID()}`,
    actorUserId: actor.id,
    actorRole: actor.role,
    action,
    targetType,
    targetId,
    details,
    timestamp: new Date().toISOString()
  };
  store.append("audits", event);
  return event;
}

function writeAudit(auth, action, targetType, targetId, details = {}) {
  return appendAudit(auth.user, action, targetType, targetId, details);
}

function writeSystemAudit(action, targetType, targetId, details = {}) {
  return appendAudit({ id: "system", role: "system" }, action, targetType, targetId, details);
}

function requireAuth(req, res, allowedRoles = []) {
  const auth = authenticateRequest(req, store);
  if (!auth) {
    sendJson(res, 401, { error: "Authentication required" });
    return null;
  }
  if (allowedRoles.length && !allowedRoles.includes(auth.user.role)) {
    sendJson(res, 403, { error: "Insufficient permission" });
    return null;
  }
  return auth;
}

function ownsResponder(auth, responderId) {
  return auth.user.role === "admin" || auth.user.responderId === responderId;
}

function canAccessIncident(auth, incidentId) {
  if (auth.user.role === "admin") return true;
  if (auth.user.role !== "responder" || !auth.user.responderId) return false;
  return store.list("alerts").some((alert) => (
    alert.incidentId === incidentId && alert.responderId === auth.user.responderId
  ));
}

async function seedAdminFromEnvironment() {
  const email = normalizeEmail(process.env.FASTAID_ADMIN_EMAIL);
  const password = process.env.FASTAID_ADMIN_PASSWORD;
  if (!email && !password) return;
  if (!email || !password) {
    console.warn("FastAid admin bootstrap requires both FASTAID_ADMIN_EMAIL and FASTAID_ADMIN_PASSWORD");
    return;
  }
  if (findUserByEmail(email)) return;
  const now = new Date().toISOString();
  store.upsert("users", {
    id: `user_${crypto.randomUUID()}`,
    email,
    name: String(process.env.FASTAID_ADMIN_NAME || "FastAid Pilot Admin").trim(),
    role: "admin",
    status: "active",
    passwordCredential: await hashPassword(password),
    source: "environment_bootstrap",
    createdAt: now,
    updatedAt: now
  });
}

async function handleSignup(req, res) {
  const body = await readBody(req);
  const email = normalizeEmail(body.email);
  const role = String(body.role || "user").trim().toLowerCase();
  const name = String(body.name || "").trim();
  if (!email || email.length > 254 || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email) || !name || name.length > 120) {
    sendJson(res, 400, { error: "A valid email and name are required" });
    return;
  }
  if (!publicSignupRoles.has(role)) {
    sendJson(res, 400, { error: "Role must be user or responder" });
    return;
  }
  if (findUserByEmail(email)) {
    sendJson(res, 409, { error: "An account already exists for this email" });
    return;
  }

  let passwordCredential;
  try {
    passwordCredential = await hashPassword(body.password);
  } catch (error) {
    sendJson(res, 400, { error: error.message });
    return;
  }

  const now = new Date().toISOString();
  const user = store.upsert("users", {
    id: `user_${crypto.randomUUID()}`,
    email,
    name,
    role,
    status: "active",
    passwordCredential,
    createdAt: now,
    updatedAt: now
  });
  const { token, session } = createSession(store, user.id, { ttlMs: sessionTtlMs });
  writeAudit({ user }, "auth.signup", "user", user.id, { role });
  sendJson(res, 201, { token, expiresAt: session.expiresAt, user: sanitizeUser(user) });
}

async function handleLogin(req, res) {
  const body = await readBody(req);
  const user = findUserByEmail(body.email);
  if (!user || user.status !== "active" || !await verifyPassword(body.password, user.passwordCredential)) {
    sendJson(res, 401, { error: "Invalid email or password" });
    return;
  }
  const { token, session } = createSession(store, user.id, { ttlMs: sessionTtlMs });
  sendJson(res, 200, { token, expiresAt: session.expiresAt, user: sanitizeUser(user) });
}

function handleLogout(res, auth) {
  writeAudit(auth, "auth.logout", "session", auth.session.id);
  store.remove("sessions", auth.session.id);
  sendJson(res, 200, { ok: true });
}

function getMime(filePath) {
  const extension = path.extname(filePath).toLowerCase();
  return {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".md": "text/markdown; charset=utf-8",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".svg": "image/svg+xml"
  }[extension] || "application/octet-stream";
}

function serveStatic(req, res, pathname) {
  const requested = pathname === "/" ? "/index-api.html" : pathname;
  const filePath = path.normalize(path.join(prototypeDir, requested));
  if (!filePath.startsWith(prototypeDir)) {
    sendJson(res, 403, { error: "Forbidden" });
    return;
  }
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    sendJson(res, 404, { error: "Not found" });
    return;
  }
  res.writeHead(200, { "content-type": getMime(filePath) });
  fs.createReadStream(filePath).pipe(res);
}

function normalizeIncidentType(value) {
  if (!value) return "Accident";
  const lower = value.toLowerCase();
  return Object.keys(incidentToTypes).find((type) => type.toLowerCase() === lower) || "Accident";
}

function normalizeLocation(value) {
  if (!value || typeof value !== "object") return null;
  const lat = Number(value.lat);
  const lng = Number(value.lng);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null;
  return {
    lat,
    lng,
    ...(value.label ? { label: String(value.label).slice(0, 120) } : {})
  };
}

function limitedText(value, maxLength) {
  return String(value || "").trim().slice(0, maxLength);
}

function parseCoordinate(value, fallback) {
  if (value === null || value === undefined || value === "") return fallback;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function toRadians(degrees) {
  return degrees * Math.PI / 180;
}

function distanceKmBetween(a, b) {
  if (!a || !b) return Number.POSITIVE_INFINITY;
  const earthRadiusKm = 6371;
  const dLat = toRadians(b.lat - a.lat);
  const dLng = toRadians(b.lng - a.lng);
  const lat1 = toRadians(a.lat);
  const lat2 = toRadians(b.lat);
  const haversine = Math.sin(dLat / 2) ** 2
    + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * earthRadiusKm * Math.asin(Math.sqrt(haversine));
}

function formatDistance(km) {
  if (!Number.isFinite(km)) return "Nearby";
  if (km < 1) return `${Math.max(50, Math.round(km * 1000 / 50) * 50)} m`;
  return `${km < 10 ? km.toFixed(1) : Math.round(km)} km`;
}

function estimateEta(km) {
  if (!Number.isFinite(km)) return "ETA pending";
  return `${Math.max(2, Math.round(km / 0.45))} min`;
}

function rankMockAid(incidentType, filterType, origin, options = {}) {
  const types = incidentToTypes[incidentType] || incidentToTypes.Accident;
  const candidates = aidPlaces.filter((place) => {
    if (filterType && filterType !== "all") return place.type === filterType;
    return types.includes(place.type);
  }).map((place) => {
    const distanceKm = distanceKmBetween(origin, place.location);
    return {
      ...place,
      distanceKm,
      openKnown: true,
      callAvailable: Boolean(place.phone),
      distance: formatDistance(distanceKm),
      eta: estimateEta(distanceKm)
    };
  }).filter((place) => {
    if (!Number.isFinite(options.maxDistanceKm)) return true;
    return place.distanceKm <= options.maxDistanceKm;
  });

  return sortAidPlaces(candidates).map(({ distanceKm, ...place }) => place);
}

function sortAidPlaces(places) {
  return places.sort((a, b) => {
    if (a.verified !== b.verified) return a.verified ? -1 : 1;
    if (a.openNow !== b.openNow) return a.openNow ? -1 : 1;
    if (a.callAvailable !== b.callAvailable) return a.callAvailable ? -1 : 1;
    const aDistance = Number.isFinite(a.distanceKm) ? a.distanceKm : Number.POSITIVE_INFINITY;
    const bDistance = Number.isFinite(b.distanceKm) ? b.distanceKm : Number.POSITIVE_INFINITY;
    return aDistance - bDistance;
  });
}

function mapIncidentToGoogleTypes(incidentType) {
  const types = incidentToTypes[incidentType] || incidentToTypes.Accident;
  return types.filter((type) => type !== "towing");
}

async function searchGooglePlaces({ lat, lng, incidentType }) {
  const key = process.env.GOOGLE_MAPS_SERVER_KEY;
  if (!key) return null;

  const includedTypes = mapIncidentToGoogleTypes(incidentType);
  const response = await fetch("https://places.googleapis.com/v1/places:searchNearby", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-goog-api-key": key,
      "x-goog-fieldmask": [
        "places.id",
        "places.displayName",
        "places.formattedAddress",
        "places.location",
        "places.types",
        "places.nationalPhoneNumber",
        "places.currentOpeningHours"
      ].join(",")
    },
    body: JSON.stringify({
      includedTypes,
      maxResultCount: 12,
      locationRestriction: {
        circle: {
          center: {
            latitude: Number(lat),
            longitude: Number(lng)
          },
          radius: 5000
        }
      }
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Google Places failed: ${response.status} ${errorText}`);
  }

  const origin = { lat: Number(lat), lng: Number(lng) };
  const data = await response.json();
  return (data.places || []).map((place, index) => {
    const type = (place.types || []).find((item) => includedTypes.includes(item)) || includedTypes[0] || "place";
    const location = place.location
      ? { lat: place.location.latitude, lng: place.location.longitude }
      : null;
    const distanceKm = distanceKmBetween(origin, location);
    return {
      id: place.id || `google-${index}`,
      name: place.displayName?.text || "Nearby place",
      type,
      distanceKm,
      icon: type === "hospital" ? "+" : type === "gas_station" ? "F" : type === "car_repair" ? "R" : "P",
      distance: formatDistance(distanceKm),
      eta: estimateEta(distanceKm),
      openNow: place.currentOpeningHours?.openNow === true,
      openKnown: typeof place.currentOpeningHours?.openNow === "boolean",
      open: typeof place.currentOpeningHours?.openNow === "boolean"
        ? (place.currentOpeningHours.openNow ? "Open now" : "Closed now")
        : "Open status unknown",
      callAvailable: Boolean(place.nationalPhoneNumber),
      verified: false,
      source: "public_place",
      phone: place.nationalPhoneNumber || "",
      address: place.formattedAddress || "",
      location
    };
  }).filter((place) => Number.isFinite(place.distanceKm))
    .sort((a, b) => a.distanceKm - b.distanceKm);
}

async function handleNearbyAid(res, url) {
  const incidentType = normalizeIncidentType(url.searchParams.get("incidentType"));
  const filterType = url.searchParams.get("type");
  const lat = parseCoordinate(url.searchParams.get("lat"), 28.6328);
  const lng = parseCoordinate(url.searchParams.get("lng"), 77.2197);
  const origin = { lat, lng };

  try {
    const googleResults = await searchGooglePlaces({ lat, lng, incidentType });
    if (googleResults && googleResults.length) {
      const verifiedMatches = rankMockAid(incidentType, filterType, origin, { maxDistanceKm: 20 })
        .filter((place) => place.verified);
      sendJson(res, 200, {
        provider: "google_places_plus_verified_mock",
        incidentType,
        results: sortAidPlaces([...verifiedMatches, ...googleResults]).map(({ distanceKm, ...place }) => place)
      });
      return;
    }
  } catch (error) {
    console.warn(error.message);
  }

  sendJson(res, 200, {
    provider: "mock",
    incidentType,
    results: rankMockAid(incidentType, filterType, origin, { maxDistanceKm: 20 })
  });
}

async function handleCreateIncident(req, res) {
  const body = await readBody(req);
  const now = new Date().toISOString();
  const incident = {
    id: `inc_${crypto.randomUUID()}`,
    type: normalizeIncidentType(body.type),
    peopleCount: Math.min(100, Math.max(0, Number(body.peopleCount || 0))),
    locationText: limitedText(body.locationText || "Current location", 200),
    location: normalizeLocation(body.location),
    note: limitedText(body.note, 1000),
    status: "created",
    createdAt: now,
    updatedAt: now
  };
  store.upsert("incidents", incident);
  const assignment = await assignNextResponder(incident);
  sendJson(res, 201, {
    incident: assignment.incident,
    alert: assignment.alert,
    notification: assignment.notification
  });
}

function reserveResponder(responder) {
  const previousStatus = responder.availabilityStatus;
  responder.availabilityStatus = "busy";
  responder.updatedAt = new Date().toISOString();
  store.upsert("responders", responder);
  writeSystemAudit("responder.reserved", "responder", responder.id, {
    from: previousStatus,
    to: responder.availabilityStatus
  });
}

function releaseResponder(responderId, auth = null, reason = "assignment_released") {
  if (!responderId) return;
  const responder = store.get("responders", responderId);
  if (!responder || responder.availabilityStatus !== "busy") return;
  responder.availabilityStatus = "available";
  responder.updatedAt = new Date().toISOString();
  store.upsert("responders", responder);
  const details = { from: "busy", to: "available", reason };
  if (auth) writeAudit(auth, "responder.availability_changed", "responder", responder.id, details);
  else writeSystemAudit("responder.availability_changed", "responder", responder.id, details);
}

async function assignNextResponder(incidentInput) {
  const incident = store.get("incidents", incidentInput.id) || incidentInput;
  const incidentAlerts = store.list("alerts").filter((alert) => alert.incidentId === incident.id);
  const activeAlert = incidentAlerts.find((alert) => activeAlertStatuses.has(alert.status));
  if (activeAlert) {
    return { incident, alert: activeAlert, notification: null, reused: true };
  }

  const compatibleTypes = incidentToTypes[normalizeIncidentType(incident.type)] || incidentToTypes.Accident;
  const attemptedResponderIds = incidentAlerts
    .map((alert) => alert.responderId)
    .filter(Boolean);
  const candidates = rankResponders({
    incident,
    responders: store.list("responders"),
    compatibleTypes,
    excludeResponderIds: attemptedResponderIds
  });

  if (!candidates.length) {
    const previousStatus = incident.status;
    incident.status = "failed";
    incident.updatedAt = new Date().toISOString();
    store.upsert("incidents", incident);
    if (previousStatus !== incident.status) {
      writeSystemAudit("incident.status_changed", "incident", incident.id, {
        from: previousStatus,
        to: incident.status,
        reason: "no_eligible_responder"
      });
    }
    return { incident, alert: null, notification: null, reused: false };
  }

  const candidate = candidates[0];
  const responder = candidate.responder;
  reserveResponder(responder);
  const now = new Date();
  const alert = {
    id: `alert_${crypto.randomUUID()}`,
    incidentId: incident.id,
    responderId: responder.id,
    status: "notified",
    responderName: responder.name,
    responderType: responder.responderType,
    attemptNumber: incidentAlerts.length + 1,
    distanceKm: Number(candidate.distanceKm.toFixed(3)),
    distance: formatResponderDistance(candidate.distanceKm),
    eta: estimateResponderEta(candidate.distanceKm),
    createdAt: now.toISOString(),
    updatedAt: now.toISOString(),
    expiresAt: new Date(now.getTime() + alertTimeoutMs).toISOString()
  };
  store.upsert("alerts", alert);

  const previousIncidentStatus = incident.status;
  incident.status = "notified";
  incident.updatedAt = now.toISOString();
  store.upsert("incidents", incident);
  writeSystemAudit("responder_alert.assigned", "alert", alert.id, {
    incidentId: incident.id,
    responderId: responder.id,
    attemptNumber: alert.attemptNumber,
    distanceKm: alert.distanceKm
  });
  writeSystemAudit("incident.status_changed", "incident", incident.id, {
    from: previousIncidentStatus,
    to: incident.status,
    sourceAlertId: alert.id
  });

  const delivery = await deliverWithRetry(
    notificationAdapter,
    { alert, incident, responder },
    {
      maxAttempts: notificationMaxAttempts,
      onAttempt: (attempt) => store.upsert("notifications", {
        ...attempt,
        alertId: alert.id,
        incidentId: incident.id,
        responderId: responder.id
      })
    }
  );

  const notification = delivery.attempts[delivery.attempts.length - 1] || null;
  if (delivery.delivered) {
    alert.notificationStatus = "delivered";
    alert.updatedAt = new Date().toISOString();
    store.upsert("alerts", alert);
    return { incident, alert, notification, reused: false };
  }

  alert.status = "notification_failed";
  alert.notificationStatus = "failed";
  alert.updatedAt = new Date().toISOString();
  store.upsert("alerts", alert);
  writeSystemAudit("responder_alert.notification_failed", "alert", alert.id, {
    responderId: responder.id,
    attempts: delivery.attempts.length
  });
  releaseResponder(responder.id, null, "notification_failed");
  const previousFailureStatus = incident.status;
  incident.status = "searching_next_responder";
  incident.updatedAt = new Date().toISOString();
  store.upsert("incidents", incident);
  writeSystemAudit("incident.status_changed", "incident", incident.id, {
    from: previousFailureStatus,
    to: incident.status,
    reason: "notification_failed",
    sourceAlertId: alert.id
  });
  return assignNextResponder(incident);
}

async function handleResponderDecision(res, alertId, decision, auth) {
  const alert = store.get("alerts", alertId);
  if (!alert) {
    sendJson(res, 404, { error: "Alert not found" });
    return;
  }
  if (alert.status !== "notified") {
    sendJson(res, 409, { error: "Alert is no longer awaiting a response" });
    return;
  }
  const now = new Date().toISOString();
  const previousAlertStatus = alert.status;
  alert.status = decision;
  alert.updatedAt = now;
  store.upsert("alerts", alert);
  writeAudit(auth, "responder_alert.status_changed", "alert", alert.id, {
    from: previousAlertStatus,
    to: decision,
    incidentId: alert.incidentId,
    responderId: alert.responderId
  });

  const incident = store.get("incidents", alert.incidentId);
  let nextAssignment = null;
  if (incident) {
    const previousIncidentStatus = incident.status;
    incident.status = decision === "accepted" ? "en_route" : "searching_next_responder";
    incident.updatedAt = now;
    store.upsert("incidents", incident);
    writeAudit(auth, "incident.status_changed", "incident", incident.id, {
      from: previousIncidentStatus,
      to: incident.status,
      sourceAlertId: alert.id
    });
    if (decision === "declined") {
      releaseResponder(alert.responderId, auth, "declined");
      nextAssignment = await assignNextResponder(incident);
    }
  }
  sendJson(res, 200, {
    alert,
    incident: nextAssignment?.incident || incident,
    nextAlert: nextAssignment?.alert || null,
    notification: nextAssignment?.notification || null
  });
}

async function handleUpdateIncident(req, res, incidentId, auth) {
  const incident = store.get("incidents", incidentId);
  if (!incident) {
    sendJson(res, 404, { error: "Incident not found" });
    return;
  }
  const body = await readBody(req);
  if (!incidentStatuses.has(body.status)) {
    sendJson(res, 400, { error: "Invalid incident status" });
    return;
  }
  const allowedTransitions = incidentTransitions.get(incident.status) || new Set();
  if (body.status !== incident.status && !allowedTransitions.has(body.status)) {
    sendJson(res, 409, {
      error: "Invalid incident status transition",
      from: incident.status,
      to: body.status
    });
    return;
  }
  const previousStatus = incident.status;
  incident.status = body.status;
  incident.updatedAt = new Date().toISOString();
  store.upsert("incidents", incident);
  writeAudit(auth, "incident.status_changed", "incident", incident.id, {
    from: previousStatus,
    to: incident.status
  });

  if (["resolved", "cancelled", "failed"].includes(incident.status)) {
    const closingStatus = incident.status === "resolved" ? "completed" : "cancelled";
    const activeAlerts = store.list("alerts").filter((alert) => (
      alert.incidentId === incident.id && activeAlertStatuses.has(alert.status)
    ));
    for (const alert of activeAlerts) {
      const previousAlertStatus = alert.status;
      alert.status = closingStatus;
      alert.updatedAt = incident.updatedAt;
      store.upsert("alerts", alert);
      writeAudit(auth, "responder_alert.status_changed", "alert", alert.id, {
        from: previousAlertStatus,
        to: closingStatus,
        incidentId: incident.id,
        responderId: alert.responderId
      });
      releaseResponder(alert.responderId, auth, `incident_${incident.status}`);
    }
  }
  sendJson(res, 200, { incident });
}

async function handleCreateResponder(req, res, auth) {
  const body = await readBody(req);
  if (!body.name || !body.responderType) {
    sendJson(res, 400, { error: "name and responderType are required" });
    return;
  }
  if (auth.user.responderId) {
    sendJson(res, 409, { error: "This account already has a responder profile" });
    return;
  }
  const now = new Date().toISOString();
  const responder = {
    id: `responder_${crypto.randomUUID()}`,
    name: limitedText(body.name, 120),
    responderType: limitedText(body.responderType, 80),
    phone: limitedText(body.phone, 40),
    verificationStatus: "pending",
    availabilityStatus: "offline",
    location: normalizeLocation(body.location),
    serviceRadiusKm: Math.min(250, Math.max(1, Number(body.serviceRadiusKm || 10))),
    source: "self_onboarding",
    userId: auth.user.id,
    createdAt: now,
    updatedAt: now
  };
  store.upsert("responders", responder);
  const user = store.get("users", auth.user.id);
  user.responderId = responder.id;
  user.updatedAt = now;
  store.upsert("users", user);
  writeAudit(auth, "responder.profile_created", "responder", responder.id, {
    responderType: responder.responderType
  });
  sendJson(res, 201, { responder });
}

async function handleResponderAvailability(req, res, responderId, auth) {
  const responder = store.get("responders", responderId);
  if (!responder) {
    sendJson(res, 404, { error: "Responder not found" });
    return;
  }
  if (!ownsResponder(auth, responderId)) {
    sendJson(res, 403, { error: "Responders can update only their own availability" });
    return;
  }
  if (responder.verificationStatus !== "verified") {
    sendJson(res, 403, { error: "Only verified responders can publish availability" });
    return;
  }
  const body = await readBody(req);
  const allowed = new Set(["available", "busy", "offline"]);
  if (!allowed.has(body.status)) {
    sendJson(res, 400, { error: "Availability must be available, busy, or offline" });
    return;
  }
  const previousStatus = responder.availabilityStatus;
  responder.availabilityStatus = body.status;
  if (body.location) responder.location = body.location;
  responder.updatedAt = new Date().toISOString();
  store.upsert("responders", responder);
  writeAudit(auth, "responder.availability_changed", "responder", responder.id, {
    from: previousStatus,
    to: responder.availabilityStatus,
    locationUpdated: Boolean(body.location)
  });
  sendJson(res, 200, { responder });
}

async function processExpiredAlerts() {
  if (alertSweepInProgress) return;
  alertSweepInProgress = true;
  try {
    const now = Date.now();
    const expiredAlerts = store.list("alerts").filter((alert) => (
      alert.status === "notified"
      && alert.expiresAt
      && new Date(alert.expiresAt).getTime() <= now
    ));

    for (const expired of expiredAlerts) {
      const alert = store.get("alerts", expired.id);
      if (!alert || alert.status !== "notified") continue;
      alert.status = "timed_out";
      alert.updatedAt = new Date().toISOString();
      store.upsert("alerts", alert);
      writeSystemAudit("responder_alert.timed_out", "alert", alert.id, {
        incidentId: alert.incidentId,
        responderId: alert.responderId
      });
      releaseResponder(alert.responderId, null, "timed_out");

      const incident = store.get("incidents", alert.incidentId);
      if (!incident || ["resolved", "cancelled", "failed"].includes(incident.status)) continue;
      const previousStatus = incident.status;
      incident.status = "searching_next_responder";
      incident.updatedAt = new Date().toISOString();
      store.upsert("incidents", incident);
      writeSystemAudit("incident.status_changed", "incident", incident.id, {
        from: previousStatus,
        to: incident.status,
        reason: "alert_timeout",
        sourceAlertId: alert.id
      });
      await assignNextResponder(incident);
    }
  } finally {
    alertSweepInProgress = false;
  }
}

async function handleResponderVerification(req, res, responderId, auth) {
  const responder = store.get("responders", responderId);
  if (!responder) {
    sendJson(res, 404, { error: "Responder not found" });
    return;
  }
  const body = await readBody(req);
  const status = String(body.status || "").trim().toLowerCase();
  if (!responderVerificationStatuses.has(status)) {
    sendJson(res, 400, { error: "Invalid responder verification status" });
    return;
  }
  const reason = limitedText(body.reason, 250);
  const previousStatus = responder.verificationStatus;
  responder.verificationStatus = status;
  if (status !== "verified") responder.availabilityStatus = "offline";
  responder.updatedAt = new Date().toISOString();
  store.upsert("responders", responder);
  writeAudit(auth, "responder.verification_changed", "responder", responder.id, {
    from: previousStatus,
    to: status,
    ...(reason ? { reason } : {})
  });
  sendJson(res, 200, { responder });
}

function getReadinessReport() {
  let storageReady = true;
  try {
    store.list("incidents");
    fs.accessSync(path.dirname(store.filePath), fs.constants.W_OK);
  } catch (error) {
    storageReady = false;
  }
  const adminConfigured = store.list("users").some((user) => (
    user.role === "admin" && user.status === "active"
  ));
  return {
    service: "FastAid backend",
    ...evaluatePilotReadiness({
      mode: deploymentMode,
      storageReady,
      publicBaseUrl,
      mapsKeyPresent: Boolean(String(process.env.GOOGLE_MAPS_SERVER_KEY || "").trim()),
      adminConfigured,
      notificationProvider: notificationAdapter.provider,
      firebaseAdapterReady: notificationAdapter.provider === "firebase"
        && typeof notificationAdapter.sendMessage === "function"
    })
  };
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);

  try {
    if (url.pathname === "/api/health") {
      sendJson(res, 200, {
        ok: true,
        service: "FastAid backend",
        mode: deploymentMode,
        mapsProvider: process.env.GOOGLE_MAPS_SERVER_KEY ? "google_places" : "mock",
        notificationProvider: notificationAdapter.provider
      });
      return;
    }

    if (url.pathname === "/api/readiness" && req.method === "GET") {
      const readiness = getReadinessReport();
      sendJson(res, readiness.ready ? 200 : 503, readiness);
      return;
    }

    if (url.pathname === "/api/countries") {
      sendJson(res, 200, countryConfig);
      return;
    }

    if (url.pathname === "/api/nearby/aid") {
      await handleNearbyAid(res, url);
      return;
    }

    if (url.pathname === "/api/auth/signup" && req.method === "POST") {
      if (!enforceRateLimit(req, res, authRateLimiter, "auth")) return;
      await handleSignup(req, res);
      return;
    }

    if (url.pathname === "/api/auth/login" && req.method === "POST") {
      if (!enforceRateLimit(req, res, authRateLimiter, "auth")) return;
      await handleLogin(req, res);
      return;
    }

    if (url.pathname === "/api/auth/logout" && req.method === "POST") {
      const auth = requireAuth(req, res);
      if (!auth) return;
      handleLogout(res, auth);
      return;
    }

    if (url.pathname === "/api/me" && req.method === "GET") {
      const auth = requireAuth(req, res);
      if (!auth) return;
      sendJson(res, 200, { user: sanitizeUser(auth.user), expiresAt: auth.session.expiresAt });
      return;
    }

    if (url.pathname === "/api/incidents" && req.method === "POST") {
      if (!enforceRateLimit(req, res, incidentRateLimiter, "incident")) return;
      await handleCreateIncident(req, res);
      return;
    }

    if (url.pathname === "/api/incidents" && req.method === "GET") {
      const auth = requireAuth(req, res, ["admin"]);
      if (!auth) return;
      const incidents = store.list("incidents")
        .sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)));
      sendJson(res, 200, { incidents });
      return;
    }

    const incidentMatch = url.pathname.match(/^\/api\/incidents\/([^/]+)$/);
    if (incidentMatch && req.method === "GET") {
      const auth = requireAuth(req, res, ["responder", "admin"]);
      if (!auth) return;
      if (!canAccessIncident(auth, incidentMatch[1])) {
        sendJson(res, 403, { error: "Responder is not assigned to this incident" });
        return;
      }
      const incident = store.get("incidents", incidentMatch[1]);
      if (!incident) sendJson(res, 404, { error: "Incident not found" });
      else sendJson(res, 200, { incident });
      return;
    }
    if (incidentMatch && req.method === "PATCH") {
      const auth = requireAuth(req, res, ["responder", "admin"]);
      if (!auth) return;
      if (!canAccessIncident(auth, incidentMatch[1])) {
        sendJson(res, 403, { error: "Responder is not assigned to this incident" });
        return;
      }
      await handleUpdateIncident(req, res, incidentMatch[1], auth);
      return;
    }

    if (url.pathname === "/api/responders" && req.method === "GET") {
      const auth = requireAuth(req, res, ["admin"]);
      if (!auth) return;
      sendJson(res, 200, { responders: store.list("responders") });
      return;
    }
    if (url.pathname === "/api/responders" && req.method === "POST") {
      const auth = requireAuth(req, res, ["responder"]);
      if (!auth) return;
      await handleCreateResponder(req, res, auth);
      return;
    }

    const availabilityMatch = url.pathname.match(/^\/api\/responders\/([^/]+)\/availability$/);
    if (availabilityMatch && req.method === "PATCH") {
      const auth = requireAuth(req, res, ["responder", "admin"]);
      if (!auth) return;
      await handleResponderAvailability(req, res, availabilityMatch[1], auth);
      return;
    }

    const responderAlertsMatch = url.pathname.match(/^\/api\/responders\/([^/]+)\/alerts$/);
    if (responderAlertsMatch && req.method === "GET") {
      const auth = requireAuth(req, res, ["responder", "admin"]);
      if (!auth) return;
      if (!ownsResponder(auth, responderAlertsMatch[1])) {
        sendJson(res, 403, { error: "Responders can retrieve only their assigned alerts" });
        return;
      }
      const alerts = store.list("alerts").filter((alert) => alert.responderId === responderAlertsMatch[1]);
      sendJson(res, 200, { alerts });
      return;
    }

    const verificationMatch = url.pathname.match(/^\/api\/admin\/responders\/([^/]+)\/verification$/);
    if (verificationMatch && req.method === "PATCH") {
      const auth = requireAuth(req, res, ["admin"]);
      if (!auth) return;
      await handleResponderVerification(req, res, verificationMatch[1], auth);
      return;
    }

    if (url.pathname === "/api/admin/duplicate-suggestions" && req.method === "GET") {
      const auth = requireAuth(req, res, ["admin"]);
      if (!auth) return;
      const suggestions = findDuplicateIncidentSuggestions(store.list("incidents"), {
        windowMinutes: url.searchParams.get("windowMinutes"),
        radiusMeters: url.searchParams.get("radiusMeters"),
        limit: url.searchParams.get("limit")
      });
      sendJson(res, 200, {
        suggestions,
        policy: { automaticMerge: false, reviewOnly: true }
      });
      return;
    }

    if (url.pathname === "/api/admin/audit" && req.method === "GET") {
      const auth = requireAuth(req, res, ["admin"]);
      if (!auth) return;
      const requestedLimit = Number(url.searchParams.get("limit") || 100);
      const limit = Math.max(1, Math.min(500, Number.isFinite(requestedLimit) ? requestedLimit : 100));
      const events = store.list("audits")
        .sort((a, b) => String(b.timestamp).localeCompare(String(a.timestamp)))
        .slice(0, limit);
      sendJson(res, 200, { events });
      return;
    }

    if (url.pathname === "/api/admin/notifications" && req.method === "GET") {
      const auth = requireAuth(req, res, ["admin"]);
      if (!auth) return;
      const attempts = store.list("notifications")
        .sort((a, b) => String(b.startedAt).localeCompare(String(a.startedAt)));
      sendJson(res, 200, { attempts });
      return;
    }

    const responderMatch = url.pathname.match(/^\/api\/responder-alerts\/([^/]+)\/(accept|decline)$/);
    if (responderMatch && req.method === "POST") {
      const auth = requireAuth(req, res, ["responder", "admin"]);
      if (!auth) return;
      const alert = store.get("alerts", responderMatch[1]);
      if (!alert) {
        sendJson(res, 404, { error: "Alert not found" });
        return;
      }
      if (!alert.responderId || !ownsResponder(auth, alert.responderId)) {
        sendJson(res, 403, { error: "Responder is not assigned to this alert" });
        return;
      }
      await handleResponderDecision(
        res,
        responderMatch[1],
        responderMatch[2] === "accept" ? "accepted" : "declined",
        auth
      );
      return;
    }

    if (req.method === "GET") {
      serveStatic(req, res, url.pathname);
      return;
    }

    sendJson(res, 405, { error: "Method not allowed" });
  } catch (error) {
    sendJson(res, 500, { error: error.message });
  }
});

async function startServer() {
  await seedAdminFromEnvironment();
  await processExpiredAlerts();
  const sweepTimer = setInterval(() => {
    processExpiredAlerts().catch((error) => console.error(error));
  }, alertSweepIntervalMs);
  sweepTimer.unref();
  server.listen(port, () => {
    console.log(`FastAid prototype running at http://localhost:${port}`);
  });
}

startServer().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

