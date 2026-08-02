const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const closedIncidentStatuses = new Set(["resolved", "cancelled", "failed"]);
const state = {
  token: null,
  user: null,
  activeView: "overview",
  incidents: [],
  responders: [],
  audits: [],
  notifications: [],
  duplicates: [],
  countries: {},
  readiness: null,
  selectedResponder: null,
  reviewIncidentIds: null
};

class ApiError extends Error {
  constructor(status, message) {
    super(message || `Request failed (${status})`);
    this.status = status;
  }
}

async function request(path, options = {}) {
  const headers = {
    accept: "application/json",
    ...(options.headers || {})
  };
  if (state.token && options.auth !== false) headers.authorization = `Bearer ${state.token}`;
  if (options.body !== undefined) headers["content-type"] = "application/json";

  const response = await fetch(path, {
    method: options.method || "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok && !options.allowErrorStatus) {
    if (response.status === 401 && state.token) signOut("Your session expired. Sign in again.", false);
    throw new ApiError(response.status, payload.error);
  }
  return { status: response.status, payload };
}

function setApiState(kind, label) {
  const chip = $("#apiStatus");
  chip.className = `status-chip ${kind}`;
  chip.textContent = label;
}

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2800);
}

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function clear(node) {
  node.replaceChildren();
}

function emptyState(container, message, error = false) {
  clear(container);
  container.append(element("div", error ? "error-state" : "empty-state", message));
}

function chip(label, kind = "neutral") {
  return element("span", `data-chip ${kind}`, label);
}

function formatLabel(value) {
  return String(value || "unknown")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatTime(value) {
  const date = new Date(value || "");
  if (Number.isNaN(date.getTime())) return "Time unavailable";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function statusKind(value) {
  if (["verified", "available", "delivered", "resolved", "arrived"].includes(value)) return "success";
  if (["pending", "notified", "accepted", "en_route", "created", "verifying"].includes(value)) return "primary";
  if (["rejected", "suspended", "failed", "cancelled", "notification_failed"].includes(value)) return "error";
  if (["busy", "searching_next_responder", "timed_out"].includes(value)) return "warning";
  return "neutral";
}

async function loadPublicStatus() {
  try {
    const [healthResult, readinessResult] = await Promise.all([
      request("/api/health", { auth: false }),
      request("/api/readiness", { auth: false, allowErrorStatus: true })
    ]);
    const health = healthResult.payload;
    state.readiness = readinessResult.payload;
    setApiState(health.ok ? "success" : "error", health.ok ? "API online" : "API unavailable");
  } catch (error) {
    setApiState("error", "API offline");
  }
}

async function signIn(event) {
  event.preventDefault();
  const button = $("#signInButton");
  const errorNode = $("#signInError");
  errorNode.hidden = true;
  button.disabled = true;
  button.textContent = "Signing in";

  try {
    const { payload } = await request("/api/auth/login", {
      method: "POST",
      auth: false,
      body: {
        email: $("#email").value.trim(),
        password: $("#password").value
      }
    });
    if (payload.user?.role !== "admin") {
      state.token = payload.token;
      await request("/api/auth/logout", { method: "POST" }).catch(() => {});
      state.token = null;
      throw new ApiError(403, "This workspace requires an administrator account.");
    }

    state.token = payload.token;
    state.user = payload.user;
    $("#password").value = "";
    $("#signInView").hidden = true;
    $("#operationsWorkspace").hidden = false;
    $("#signedIn").hidden = false;
    $("#signedInUser").textContent = payload.user.name || payload.user.email;
    setApiState("success", "Admin session active");
    await loadOperations();
    $("#pageTitle").focus?.();
  } catch (error) {
    errorNode.textContent = error.message || "Sign in failed.";
    errorNode.hidden = false;
    setApiState(error.status === 401 ? "warning" : "error", "Sign in required");
  } finally {
    button.disabled = false;
    button.textContent = "Sign in";
  }
}

async function signOut(message = "Signed out", callApi = true) {
  const token = state.token;
  state.token = null;
  state.user = null;
  if (callApi && token) {
    state.token = token;
    await request("/api/auth/logout", { method: "POST" }).catch(() => {});
    state.token = null;
  }
  $("#operationsWorkspace").hidden = true;
  $("#signedIn").hidden = true;
  $("#signInView").hidden = false;
  $("#signInForm").reset();
  $("#signInError").hidden = true;
  setApiState("warning", "Sign in required");
  showToast(message);
  $("#email").focus();
}

async function loadOperations() {
  setApiState("neutral", "Refreshing data");
  const resources = [
    ["incidents", "/api/incidents", "incidents"],
    ["responders", "/api/responders", "responders"],
    ["audits", "/api/admin/audit?limit=250", "events"],
    ["notifications", "/api/admin/notifications", "attempts"],
    ["duplicates", "/api/admin/duplicate-suggestions?limit=100", "suggestions"],
    ["countries", "/api/countries", null],
    ["readiness", "/api/readiness", null, true]
  ];

  const results = await Promise.allSettled(resources.map(([, path, , allowErrorStatus]) => (
    request(path, { allowErrorStatus })
  )));
  const failures = [];

  results.forEach((result, index) => {
    const [stateKey, , payloadKey] = resources[index];
    if (result.status === "fulfilled") {
      const payload = result.value.payload;
      state[stateKey] = payloadKey ? (payload[payloadKey] || []) : payload;
    } else {
      failures.push(stateKey);
    }
  });

  renderAll();
  setApiState(failures.length ? "warning" : "success", failures.length ? "Some data unavailable" : "Operations online");
  if (failures.length) showToast(`Could not refresh: ${failures.join(", ")}`);
}

function renderAll() {
  renderMetrics();
  renderOverview();
  renderIncidents();
  renderDuplicates();
  renderResponders();
  renderAudit();
  renderReadiness($("#systemReadiness"));
  renderNotifications();
  renderCountries();
}

function renderMetrics() {
  const open = state.incidents.filter((incident) => !closedIncidentStatuses.has(incident.status));
  const pending = state.responders.filter((responder) => responder.verificationStatus === "pending");
  $("#openIncidentMetric").textContent = String(open.length);
  $("#openIncidentDetail").textContent = open.length === 1 ? "1 incident requires tracking" : `${open.length} incidents require tracking`;
  $("#pendingResponderMetric").textContent = String(pending.length);
  $("#pendingResponderDetail").textContent = pending.length ? "Verification queue" : "Queue clear";
  $("#duplicateMetric").textContent = String(state.duplicates.length);
  $("#readinessMetric").textContent = state.readiness?.ready ? "Ready" : "Needs config";
  $("#readinessDetail").textContent = state.readiness?.mode ? `${formatLabel(state.readiness.mode)} mode` : "Readiness unavailable";
}

function renderOverview() {
  const incidents = state.incidents
    .filter((incident) => !closedIncidentStatuses.has(incident.status))
    .slice(0, 5);
  renderIncidentRows($("#overviewIncidents"), incidents, { compact: true });

  const responders = state.responders
    .filter((responder) => responder.verificationStatus !== "verified")
    .sort((a, b) => verificationRank(a.verificationStatus) - verificationRank(b.verificationStatus))
    .slice(0, 5);
  renderResponderRows($("#overviewResponders"), responders, { compact: true });
  renderReadiness($("#overviewReadiness"));
}

function filteredIncidents() {
  const query = $("#incidentSearch").value.trim().toLowerCase();
  const status = $("#incidentStatusFilter").value;
  return state.incidents.filter((incident) => {
    if (state.reviewIncidentIds && !state.reviewIncidentIds.has(incident.id)) return false;
    const searchable = [incident.id, incident.type, incident.locationText, incident.status]
      .join(" ").toLowerCase();
    if (query && !searchable.includes(query)) return false;
    if (status === "active" && closedIncidentStatuses.has(incident.status)) return false;
    if (!["all", "active"].includes(status) && incident.status !== status) return false;
    return true;
  });
}

function renderIncidents() {
  renderIncidentRows($("#incidentList"), filteredIncidents());
}

function renderIncidentRows(container, incidents, options = {}) {
  clear(container);
  if (!incidents.length) {
    emptyState(container, state.incidents.length ? "No incidents match this view." : "No incidents have been recorded.");
    return;
  }

  const duplicateIds = new Set(state.duplicates.flatMap((suggestion) => suggestion.incidentIds || []));
  incidents.forEach((incident) => {
    const row = element("article", "data-row");
    const primary = element("div", "row-primary");
    primary.append(
      element("strong", "", `${incident.type || "Incident"} · ${Number(incident.peopleCount || 0)} people`),
      element("span", "", incident.locationText || "Location not supplied"),
      element("small", "", `${formatTime(incident.createdAt)} · ${incident.id}`)
    );
    const actions = element("div", "row-actions");
    actions.append(chip(formatLabel(incident.status), statusKind(incident.status)));
    if (duplicateIds.has(incident.id)) actions.append(chip("Review pair", "warning"));
    row.append(primary, actions);
    container.append(row);
  });
}

function renderDuplicates() {
  const container = $("#duplicateList");
  clear(container);
  if (!state.duplicates.length) {
    emptyState(container, "No possible duplicate pairs in the current review window.");
    return;
  }
  const incidentsById = new Map(state.incidents.map((incident) => [incident.id, incident]));
  state.duplicates.forEach((suggestion) => {
    const incidents = (suggestion.incidentIds || []).map((id) => incidentsById.get(id)).filter(Boolean);
    const row = element("article", "data-row");
    const primary = element("div", "row-primary");
    const title = incidents.map((incident) => incident.type || "Incident").join(" / ") || "Incident pair";
    primary.append(
      element("strong", "", title),
      element("span", "", suggestion.reason || "Nearby reports within the review window"),
      element("small", "", `${suggestion.timeDeltaMinutes ?? "?"} min apart · review only`)
    );
    const actions = element("div", "row-actions");
    actions.append(chip(formatLabel(suggestion.confidence), suggestion.confidence === "high" ? "warning" : "neutral"));
    const review = element("button", "outlined-button", "Review pair");
    review.type = "button";
    review.addEventListener("click", () => reviewPair(suggestion.incidentIds));
    actions.append(review);
    row.append(primary, actions);
    container.append(row);
  });
}

function reviewPair(incidentIds) {
  state.reviewIncidentIds = new Set(incidentIds || []);
  $("#incidentSearch").value = "";
  $("#incidentStatusFilter").value = "all";
  switchView("incidents");
  renderIncidents();
  showToast("Showing the suggested pair. No incident was changed.");
}

function filteredResponders() {
  const query = $("#responderSearch").value.trim().toLowerCase();
  const verification = $("#verificationFilter").value;
  return state.responders.filter((responder) => {
    const searchable = [responder.id, responder.name, responder.responderType, responder.phone]
      .join(" ").toLowerCase();
    if (query && !searchable.includes(query)) return false;
    return verification === "all" || responder.verificationStatus === verification;
  }).sort((a, b) => (
    verificationRank(a.verificationStatus) - verificationRank(b.verificationStatus)
    || String(a.name).localeCompare(String(b.name))
  ));
}

function verificationRank(status) {
  return { pending: 0, suspended: 1, rejected: 2, verified: 3 }[status] ?? 4;
}

function renderResponders() {
  renderResponderRows($("#responderList"), filteredResponders());
}

function renderResponderRows(container, responders, options = {}) {
  clear(container);
  if (!responders.length) {
    emptyState(container, state.responders.length ? "No responders match this view." : "No responder profiles exist.");
    return;
  }
  responders.forEach((responder) => {
    const row = element("article", "data-row");
    const primary = element("div", "row-primary");
    primary.append(
      element("strong", "", responder.name || "Unnamed responder"),
      element("span", "", `${formatLabel(responder.responderType)} · ${responder.phone || "No phone supplied"}`),
      element("small", "", responder.id)
    );
    const actions = element("div", "row-actions");
    actions.append(
      chip(formatLabel(responder.verificationStatus), statusKind(responder.verificationStatus)),
      chip(formatLabel(responder.availabilityStatus), statusKind(responder.availabilityStatus))
    );
    if (!options.compact) {
      const change = element("button", "outlined-button", "Change status");
      change.type = "button";
      change.addEventListener("click", () => openStatusDialog(responder));
      actions.append(change);
    }
    row.append(primary, actions);
    container.append(row);
  });
}

function openStatusDialog(responder) {
  state.selectedResponder = responder;
  $("#statusResponderName").textContent = `${responder.name} · ${formatLabel(responder.responderType)}`;
  $("#newVerificationStatus").value = responder.verificationStatus || "pending";
  $("#verificationReason").value = "";
  updateStatusWarning();
  $("#statusDialog").showModal();
  $("#newVerificationStatus").focus();
}

function updateStatusWarning() {
  const status = $("#newVerificationStatus").value;
  const warning = $("#statusWarning");
  warning.textContent = status === "verified"
    ? "Verification permits future availability; it does not dispatch an active request."
    : "This status forces the responder offline and prevents new assignments.";
}

async function applyResponderStatus(event) {
  event.preventDefault();
  const responder = state.selectedResponder;
  if (!responder) return;
  const status = $("#newVerificationStatus").value;
  const reason = $("#verificationReason").value.trim();
  if (!reason) {
    $("#statusWarning").textContent = "Add an audit note before applying this status.";
    $("#verificationReason").focus();
    return;
  }

  const submit = $("#statusForm button[type='submit']");
  submit.disabled = true;
  try {
    await request(`/api/admin/responders/${encodeURIComponent(responder.id)}/verification`, {
      method: "PATCH",
      body: { status, reason }
    });
    $("#statusDialog").close();
    state.selectedResponder = null;
    showToast(`${responder.name} is now ${formatLabel(status).toLowerCase()}.`);
    await loadOperations();
  } catch (error) {
    $("#statusWarning").textContent = error.message || "Could not update responder status.";
  } finally {
    submit.disabled = false;
  }
}

function renderAudit() {
  const container = $("#auditList");
  const filter = $("#auditFilter").value;
  const events = state.audits.filter((event) => filter === "all" || String(event.action).startsWith(`${filter}.`));
  clear(container);
  if (!events.length) {
    emptyState(container, state.audits.length ? "No audit events match this filter." : "No protected operations have been recorded.");
    return;
  }
  events.forEach((event) => {
    const row = element("article", "audit-row");
    const time = element("time", "", formatTime(event.timestamp));
    time.dateTime = event.timestamp || "";
    const summary = element("div", "row-primary");
    summary.append(
      element("strong", "", formatLabel(event.action)),
      element("span", "", `${formatLabel(event.actorRole)} · ${event.targetType || "record"}`)
    );
    const target = element("code", "", event.targetId || event.id || "-");
    row.append(time, summary, target);
    container.append(row);
  });
}

function renderReadiness(container) {
  clear(container);
  const checks = state.readiness?.checks || {};
  if (!Object.keys(checks).length) {
    emptyState(container, "Readiness data is unavailable.", true);
    return;
  }
  Object.entries(checks).forEach(([name, check]) => {
    const item = element("article", "check-item");
    const line = element("div", "status-line");
    line.append(element("strong", "", formatLabel(name)));
    line.append(chip(check.ready ? "Ready" : (check.required ? "Required" : "Optional"), check.ready ? "success" : (check.required ? "error" : "warning")));
    item.append(line, element("span", "", check.detail || "No detail supplied"));
    container.append(item);
  });
}

function renderNotifications() {
  const container = $("#notificationList");
  clear(container);
  if (!state.notifications.length) {
    emptyState(container, "No notification attempts have been recorded.");
    return;
  }
  state.notifications.slice(0, 8).forEach((attempt) => {
    const row = element("article", "data-row");
    const primary = element("div", "row-primary");
    primary.append(
      element("strong", "", `${formatLabel(attempt.provider)} delivery`),
      element("span", "", attempt.error || `Alert ${attempt.alertId || "unknown"}`),
      element("small", "", formatTime(attempt.startedAt || attempt.completedAt))
    );
    const actions = element("div", "row-actions");
    actions.append(chip(attempt.delivered ? "Delivered" : "Failed", attempt.delivered ? "success" : "error"));
    row.append(primary, actions);
    container.append(row);
  });
}

function renderCountries() {
  const container = $("#countryList");
  clear(container);
  const countries = Object.entries(state.countries || {});
  if (!countries.length) {
    emptyState(container, "Country configuration is unavailable.", true);
    return;
  }
  countries.forEach(([code, config]) => {
    const item = element("article", "country-item");
    item.append(
      element("strong", "", `${config.country} · ${config.emergencyNumber}`),
      element("span", "", `${code} · ${config.distanceUnit} · ${(config.poiCategories || []).length} aid categories`)
    );
    container.append(item);
  });
}

function switchView(view) {
  if (!$("#operationsWorkspace") || $("#operationsWorkspace").hidden) return;
  state.activeView = view;
  $$("[data-view-panel]").forEach((panel) => {
    const active = panel.dataset.viewPanel === view;
    panel.hidden = !active;
    panel.classList.toggle("active", active);
  });
  $$("[data-view]").forEach((button) => {
    const active = button.dataset.view === view;
    button.classList.toggle("active", active);
    if (active) button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });
  const title = { overview: "Overview", incidents: "Incidents", responders: "Responders", audit: "Audit", system: "System" }[view] || "Operations";
  $("#pageTitle").textContent = title;
  const heading = $(`[data-view-panel="${view}"] h2[tabindex="-1"]`);
  if (heading) heading.focus();
}

function clearPairReview() {
  if (!state.reviewIncidentIds) return;
  state.reviewIncidentIds = null;
  renderIncidents();
}

function bindEvents() {
  $("#signInForm").addEventListener("submit", signIn);
  $("#logoutButton").addEventListener("click", () => signOut());
  $("#refreshButton").addEventListener("click", loadOperations);

  $$("[data-view]").forEach((button) => button.addEventListener("click", () => switchView(button.dataset.view)));
  $$("[data-open-view]").forEach((button) => button.addEventListener("click", () => switchView(button.dataset.openView)));

  $("#incidentSearch").addEventListener("input", () => {
    clearPairReview();
    renderIncidents();
  });
  $("#incidentStatusFilter").addEventListener("change", () => {
    clearPairReview();
    renderIncidents();
  });
  $("#responderSearch").addEventListener("input", renderResponders);
  $("#verificationFilter").addEventListener("change", renderResponders);
  $("#auditFilter").addEventListener("change", renderAudit);

  $("#newVerificationStatus").addEventListener("change", updateStatusWarning);
  $("#statusForm").addEventListener("submit", applyResponderStatus);
  $("#closeStatusDialog").addEventListener("click", () => $("#statusDialog").close());
  $("#cancelStatusDialog").addEventListener("click", () => $("#statusDialog").close());
  $("#statusDialog").addEventListener("close", () => {
    state.selectedResponder = null;
  });
}

bindEvents();
loadPublicStatus();
